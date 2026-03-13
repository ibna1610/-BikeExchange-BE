package com.bikeexchange.service.service;

import com.bikeexchange.dto.request.DisputeResolutionType;
import com.bikeexchange.dto.request.ReturnDisputeRequest;
import com.bikeexchange.exception.InvalidOrderStatusException;
import com.bikeexchange.exception.ResourceNotFoundException;
import com.bikeexchange.model.Bike;
import com.bikeexchange.model.Dispute;
import com.bikeexchange.model.Order;
import com.bikeexchange.model.User;
import com.bikeexchange.repository.BikeRepository;
import com.bikeexchange.repository.DisputeRepository;
import com.bikeexchange.repository.OrderRepository;
import com.bikeexchange.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DisputeService {

    @Autowired
    private DisputeRepository disputeRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BikeRepository bikeRepository;

    @Autowired
    private OrderService orderService;

    @Transactional(readOnly = true)
    public List<Dispute> getPendingDisputes() {
        return disputeRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(Dispute.DisputeStatus.OPEN, Dispute.DisputeStatus.INVESTIGATING));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Dispute createReturnDispute(Long orderId, Long buyerId, ReturnDisputeRequest request) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new IllegalArgumentException("Only the buyer can open a return dispute");
        }

        if (order.getStatus() != Order.OrderStatus.RETURN_REQUESTED) {
            throw new InvalidOrderStatusException("Order must be in RETURN_REQUESTED status to open return dispute");
        }

        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Dispute reason is required");
        }

        if (request.getBuyerAddress() == null || request.getBuyerAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Buyer address is required");
        }

        if (request.getBuyerPhone() == null || request.getBuyerPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Buyer phone is required");
        }

        if (request.getBuyerEmail() == null || request.getBuyerEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Buyer email is required");
        }

        boolean hasActiveDispute = disputeRepository.existsByOrderIdAndStatusIn(
                order.getId(),
                java.util.List.of(Dispute.DisputeStatus.OPEN, Dispute.DisputeStatus.INVESTIGATING));
        if (hasActiveDispute) {
            throw new IllegalStateException("This order already has an active dispute");
        }

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));

        order.setStatus(Order.OrderStatus.DISPUTED);
        orderRepository.save(order);

        Dispute dispute = new Dispute();
        dispute.setOrder(order);
        dispute.setReporter(buyer);
        dispute.setReason(request.getReason());
        dispute.setStatus(Dispute.DisputeStatus.OPEN);
        dispute.setDisputeType(Dispute.DisputeType.RETURN);
        dispute.setBuyerContactAddress(request.getBuyerAddress().trim());
        dispute.setBuyerContactPhone(request.getBuyerPhone().trim());
        dispute.setBuyerContactEmail(request.getBuyerEmail().trim());
        dispute.setCreatedAt(LocalDateTime.now());
        return disputeRepository.save(dispute);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Dispute resolveDispute(Long disputeId, DisputeResolutionType resolutionType, String resolutionNote) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found"));

        if (dispute.getStatus() != Dispute.DisputeStatus.OPEN
                && dispute.getStatus() != Dispute.DisputeStatus.INVESTIGATING) {
            throw new IllegalStateException("Dispute is already resolved");
        }

        Order order = orderRepository.findByIdForUpdate(dispute.getOrder().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != Order.OrderStatus.DISPUTED) {
            throw new InvalidOrderStatusException("Order is not in DISPUTED status");
        }

        if (resolutionType == null) {
            throw new IllegalArgumentException("resolutionType is required");
        }

        if (dispute.getDisputeType() == Dispute.DisputeType.RETURN) {
            if (resolutionType == DisputeResolutionType.REFUND) {
                orderService.refundToBuyer(order, "Return approved - Refund for Dispute: " + disputeId);

                Bike bike = order.getBike();
                bike.setStatus(Bike.BikeStatus.ACTIVE);
                bikeRepository.save(bike);

                order.setStatus(Order.OrderStatus.REFUNDED);
                orderRepository.save(order);

                dispute.setStatus(Dispute.DisputeStatus.RESOLVED_REFUND);
            } else if (resolutionType == DisputeResolutionType.RELEASE) {
                orderService.releaseToSeller(order, "Return denied - Release for Dispute: " + disputeId);
                dispute.setStatus(Dispute.DisputeStatus.RESOLVED_RELEASE);
            } else {
                throw new IllegalArgumentException("For return disputes, resolutionType must be REFUND or RELEASE");
            }
        } else {
            if (resolutionType == DisputeResolutionType.REFUND) {
                orderService.refundToBuyer(order, "Refund for Dispute: " + disputeId);

                Bike bike = order.getBike();
                bike.setStatus(Bike.BikeStatus.ACTIVE);
                bikeRepository.save(bike);

                order.setStatus(Order.OrderStatus.REFUNDED);
                orderRepository.save(order);

                dispute.setStatus(Dispute.DisputeStatus.RESOLVED_REFUND);
            } else if (resolutionType == DisputeResolutionType.RELEASE) {
                orderService.releaseToSeller(order, "Release for Dispute: " + disputeId);
                dispute.setStatus(Dispute.DisputeStatus.RESOLVED_RELEASE);
            } else {
                throw new IllegalArgumentException("Unknown resolution type: " + resolutionType);
            }
        }
        dispute.setResolutionNote(resolutionNote);
        dispute.setResolvedAt(LocalDateTime.now());
        return disputeRepository.save(dispute);
    }
}
