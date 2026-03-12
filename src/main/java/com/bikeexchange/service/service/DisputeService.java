package com.bikeexchange.service.service;

import com.bikeexchange.dto.request.DisputeCreateRequest;
import com.bikeexchange.dto.request.DisputeResolveRequest;
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

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Dispute createDispute(Long reporterId, DisputeCreateRequest request) {
        Order order = orderRepository.findByIdForUpdate(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Dispute reason is required");
        }

        if (!order.getBuyer().getId().equals(reporterId)
                && !order.getBike().getSeller().getId().equals(reporterId)) {
            throw new IllegalArgumentException("Only buyer or seller can open a dispute");
        }

        if (order.getStatus() != Order.OrderStatus.ESCROWED
                && order.getStatus() != Order.OrderStatus.ACCEPTED
                && order.getStatus() != Order.OrderStatus.DELIVERED
                && order.getStatus() != Order.OrderStatus.RETURN_REQUESTED) {
            throw new InvalidOrderStatusException("Cannot dispute an order in status: " + order.getStatus());
        }

            boolean hasActiveDispute = disputeRepository.existsByOrderIdAndStatusIn(
                order.getId(),
                java.util.List.of(Dispute.DisputeStatus.OPEN, Dispute.DisputeStatus.INVESTIGATING));
            if (hasActiveDispute) {
                throw new IllegalStateException("This order already has an active dispute");
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
        dispute.setDisputeType(Dispute.DisputeType.GENERAL);
        dispute.setCreatedAt(LocalDateTime.now());
        return disputeRepository.save(dispute);
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
    public Dispute resolveDispute(Long disputeId, DisputeResolveRequest request) {
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

        String resolutionType = request.getResolutionType() == null
                ? ""
                : request.getResolutionType().trim().toUpperCase();

        if (dispute.getDisputeType() == Dispute.DisputeType.RETURN) {
            // Return dispute: APPROVE_REFUND or DENY
            if ("APPROVE_REFUND".equals(resolutionType)) {
                orderService.refundToBuyer(order, "Return approved - Refund for Dispute: " + disputeId);

                Bike bike = order.getBike();
                bike.setStatus(Bike.BikeStatus.ACTIVE);
                bikeRepository.save(bike);

                order.setStatus(Order.OrderStatus.REFUNDED);
                orderRepository.save(order);

                dispute.setStatus(Dispute.DisputeStatus.RESOLVED_REFUND);
            } else if ("DENY".equals(resolutionType)) {
                orderService.releaseToSeller(order, "Return denied - Release for Dispute: " + disputeId);
                dispute.setStatus(Dispute.DisputeStatus.RESOLVED_RELEASE);
            } else {
                throw new IllegalArgumentException("For return disputes, resolutionType must be APPROVE_REFUND or DENY");
            }
        } else {
            // General dispute: REFUND or RELEASE
            if ("REFUND".equals(resolutionType)) {
                orderService.refundToBuyer(order, "Refund for Dispute: " + disputeId);

                Bike bike = order.getBike();
                bike.setStatus(Bike.BikeStatus.ACTIVE);
                bikeRepository.save(bike);

                order.setStatus(Order.OrderStatus.REFUNDED);
                orderRepository.save(order);

                dispute.setStatus(Dispute.DisputeStatus.RESOLVED_REFUND);
            } else if ("RELEASE".equals(resolutionType)) {
                orderService.releaseToSeller(order, "Release for Dispute: " + disputeId);
                dispute.setStatus(Dispute.DisputeStatus.RESOLVED_RELEASE);
            } else {
                throw new IllegalArgumentException("Unknown resolution type: " + request.getResolutionType());
            }
        }
        dispute.setResolutionNote(request.getResolutionNote());
        dispute.setResolvedAt(LocalDateTime.now());
        return disputeRepository.save(dispute);
    }
}
