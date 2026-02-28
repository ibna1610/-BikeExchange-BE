package com.bikeexchange.service;

import com.bikeexchange.exception.InsufficientBalanceException;
import com.bikeexchange.exception.InvalidOrderStatusException;
import com.bikeexchange.exception.ListingNotAvailableException;
import com.bikeexchange.exception.ResourceNotFoundException;
import com.bikeexchange.model.*;
import com.bikeexchange.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserWalletRepository walletRepository;

    @Autowired
    private BikeRepository bikeRepository;

    @Autowired
    private PointTransactionRepository pointTxRepo;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Order createOrder(Long buyerId, Long bikeId, String idempotencyKey) {
        // 1. Check idempotency
        if (orderRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new IllegalArgumentException("Order with this idempotency key already exists in progress");
        }

        // 2. Lock bike
        Bike bike = bikeRepository.findByIdForUpdate(bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike missing"));

        if (bike.getStatus() != Bike.BikeStatus.ACTIVE && bike.getStatus() != Bike.BikeStatus.VERIFIED) {
            throw new ListingNotAvailableException("Bike is not ACTIVE or VERIFIED");
        }

        // 3. Lock buyer wallet
        UserWallet buyerWallet = walletRepository.findByUserIdForUpdate(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer wallet missing"));

        if (buyerWallet.getAvailablePoints() < bike.getPricePoints()) {
            throw new InsufficientBalanceException("Not enough available points to escrow");
        }

        // 4. Update balances & Bike
        buyerWallet.setAvailablePoints(buyerWallet.getAvailablePoints() - bike.getPricePoints());
        buyerWallet.setFrozenPoints(buyerWallet.getFrozenPoints() + bike.getPricePoints());
        walletRepository.save(buyerWallet);

        bike.setStatus(Bike.BikeStatus.RESERVED);
        bikeRepository.save(bike);

        // 5. Point Transaction log
        PointTransaction pTx = new PointTransaction();
        pTx.setUser(buyerWallet.getUser());
        pTx.setAmount(bike.getPricePoints());
        pTx.setType(PointTransaction.TransactionType.ESCROW_HOLD);
        pTx.setStatus(PointTransaction.TransactionStatus.SUCCESS);
        pTx.setReferenceId("OrderKey: " + idempotencyKey);
        pointTxRepo.save(pTx);

        // 6. Save Order
        Order order = new Order();
        order.setBuyer(buyerWallet.getUser());
        order.setBike(bike);
        order.setAmountPoints(bike.getPricePoints());
        order.setIdempotencyKey(idempotencyKey);
        order.setStatus(Order.OrderStatus.ESCROWED);

        return orderRepository.save(order);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Order approveOrder(Long orderId, Long buyerId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order missing"));

        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new IllegalArgumentException("Only buyer can approve");
        }

        if (order.getStatus() != Order.OrderStatus.ESCROWED) {
            throw new InvalidOrderStatusException("Order is not in ESCROWED state");
        }

        Long total = order.getAmountPoints();
        Long adminCommission = (long) (total * 0.05); // 5%
        Long sellerRevenue = total - adminCommission;

        UserWallet buyerWallet = walletRepository.findByUserIdForUpdate(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer wallet missing"));

        UserWallet sellerWallet = walletRepository.findByUserIdForUpdate(order.getBike().getSeller().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller wallet missing"));

        // Release buyer's frozen points effectively
        buyerWallet.setFrozenPoints(buyerWallet.getFrozenPoints() - total);
        walletRepository.save(buyerWallet);

        // Add to seller available
        sellerWallet.setAvailablePoints(sellerWallet.getAvailablePoints() + sellerRevenue);
        walletRepository.save(sellerWallet);

        // System wallet logic omitted / could log Admin Commission in PointTransaction

        // Log transaction for seller
        PointTransaction pTx = new PointTransaction();
        pTx.setUser(sellerWallet.getUser());
        pTx.setAmount(sellerRevenue);
        pTx.setType(PointTransaction.TransactionType.EARN);
        pTx.setStatus(PointTransaction.TransactionStatus.SUCCESS);
        pTx.setReferenceId("Order: " + orderId);
        pointTxRepo.save(pTx);

        order.setStatus(Order.OrderStatus.COMPLETED);

        Bike listing = order.getBike();
        listing.setStatus(Bike.BikeStatus.SOLD);
        bikeRepository.save(listing);

        return orderRepository.save(order);
    }
}
