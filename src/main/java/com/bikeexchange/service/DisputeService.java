package com.bikeexchange.service;

import com.bikeexchange.dto.request.DisputeCreateRequest;
import com.bikeexchange.dto.request.DisputeResolveRequest;
import com.bikeexchange.exception.InvalidOrderStatusException;
import com.bikeexchange.exception.ResourceNotFoundException;
import com.bikeexchange.model.*;
import com.bikeexchange.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DisputeService {

    @Autowired
    private DisputeRepository disputeRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserWalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BikeRepository bikeRepository;

    @Autowired
    private PointTransactionRepository pointTxRepo;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Dispute createDispute(Long reporterId, DisputeCreateRequest request) {
        Order order = orderRepository.findByIdForUpdate(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getBuyer().getId().equals(reporterId)
                && !order.getListing().getSeller().getId().equals(reporterId)) {
            throw new IllegalArgumentException("Only buyer or seller can open a dispute");
        }

        if (order.getStatus() != Order.OrderStatus.ESCROWED) {
            throw new InvalidOrderStatusException("Can only dispute orders that are in ESCROWED state");
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Reporter not found"));

        order.setStatus(Order.OrderStatus.DISPUTED);
        orderRepository.save(order);

        Dispute dispute = new Dispute();
        dispute.setOrder(order);
        dispute.setReporter(reporter);
        dispute.setReason(request.getReason());
        dispute.setStatus(Dispute.DisputeStatus.OPEN);
        dispute.setCreatedAt(LocalDateTime.now());

        return disputeRepository.save(dispute);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Dispute resolveDispute(Long disputeId, DisputeResolveRequest request) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found"));

        if (dispute.getStatus() != Dispute.DisputeStatus.OPEN
                && dispute.getStatus() != Dispute.DisputeStatus.INVESTIGATING) {
            throw new IllegalStateException("Dispute is already resolved");
        }

        Order order = dispute.getOrder();
        // Relock the order to be safe, though Order should be in DISPUTED state
        order = orderRepository.findByIdForUpdate(order.getId()).orElse(order);

        Long amount = order.getAmountPoints();
        User buyer = order.getBuyer();
        User seller = order.getListing().getSeller();

        UserWallet buyerWallet = walletRepository.findByUserIdForUpdate(buyer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Buyer wallet not found"));

        UserWallet sellerWallet = walletRepository.findByUserIdForUpdate(seller.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller wallet not found"));

        if ("REFUND".equalsIgnoreCase(request.getResolutionType())) {
            // Refund buyer: frozen points -> available points
            buyerWallet.setFrozenPoints(buyerWallet.getFrozenPoints() - amount);
            buyerWallet.setAvailablePoints(buyerWallet.getAvailablePoints() + amount);
            walletRepository.save(buyerWallet);

            PointTransaction tx = new PointTransaction();
            tx.setUser(buyer);
            tx.setAmount(amount);
            tx.setType(PointTransaction.TransactionType.ESCROW_RELEASE); // refund
            tx.setStatus(PointTransaction.TransactionStatus.SUCCESS);
            tx.setReferenceId("Refund for Dispute: " + disputeId);
            pointTxRepo.save(tx);

            order.setStatus(Order.OrderStatus.CANCELLED);
            orderRepository.save(order);

            Bike listing = order.getListing();
            listing.setStatus(Bike.BikeStatus.ACTIVE); // make available again
            bikeRepository.save(listing);

            dispute.setStatus(Dispute.DisputeStatus.RESOLVED_REFUND);
            dispute.setResolutionNote(request.getResolutionNote());

        } else if ("RELEASE".equalsIgnoreCase(request.getResolutionType())) {
            // Release to seller: buyer frozen -> seller available (- commission)
            Long commission = (long) (amount * 0.05);
            Long sellerRevenue = amount - commission;

            buyerWallet.setFrozenPoints(buyerWallet.getFrozenPoints() - amount);
            walletRepository.save(buyerWallet);

            sellerWallet.setAvailablePoints(sellerWallet.getAvailablePoints() + sellerRevenue);
            walletRepository.save(sellerWallet);

            PointTransaction tx = new PointTransaction();
            tx.setUser(seller);
            tx.setAmount(sellerRevenue);
            tx.setType(PointTransaction.TransactionType.EARN);
            tx.setStatus(PointTransaction.TransactionStatus.SUCCESS);
            tx.setReferenceId("Release for Dispute: " + disputeId);
            pointTxRepo.save(tx);

            order.setStatus(Order.OrderStatus.COMPLETED);
            orderRepository.save(order);

            Bike listing = order.getListing();
            listing.setStatus(Bike.BikeStatus.SOLD);
            bikeRepository.save(listing);

            dispute.setStatus(Dispute.DisputeStatus.RESOLVED_RELEASE);
            dispute.setResolutionNote(request.getResolutionNote());
        } else {
            throw new IllegalArgumentException("Unknown resolution type: " + request.getResolutionType());
        }

        dispute.setResolvedAt(LocalDateTime.now());
        return disputeRepository.save(dispute);
    }
}
