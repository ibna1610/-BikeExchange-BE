package com.bikeexchange.service.service;

import com.bikeexchange.exception.InsufficientBalanceException;
import com.bikeexchange.exception.InvalidOrderStatusException;
import com.bikeexchange.exception.ListingNotAvailableException;
import com.bikeexchange.exception.ResourceNotFoundException;
import com.bikeexchange.model.Bike;
import com.bikeexchange.model.Order;
import com.bikeexchange.model.PointTransaction;
import com.bikeexchange.model.User;
import com.bikeexchange.model.UserWallet;
import com.bikeexchange.repository.BikeRepository;
import com.bikeexchange.repository.OrderRepository;
import com.bikeexchange.repository.PointTransactionRepository;
import com.bikeexchange.repository.UserWalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    @Autowired
    private HistoryService historyService;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Order createOrder(Long buyerId, Long bikeId, String idempotencyKey) {
        if (orderRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new IllegalArgumentException("Duplicate idempotency key");
        }

        Bike bike = bikeRepository.findByIdForUpdate(bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike not found"));

        if (bike.getStatus() != Bike.BikeStatus.ACTIVE && bike.getStatus() != Bike.BikeStatus.VERIFIED) {
            throw new ListingNotAvailableException("Bike is not available for purchase");
        }

        UserWallet buyerWallet = walletRepository.findByUserIdForUpdate(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer wallet not found"));

        if (buyerWallet.getAvailablePoints() < bike.getPricePoints()) {
            throw new InsufficientBalanceException("Not enough points");
        }

        buyerWallet.setAvailablePoints(buyerWallet.getAvailablePoints() - bike.getPricePoints());
        buyerWallet.setFrozenPoints(buyerWallet.getFrozenPoints() + bike.getPricePoints());
        walletRepository.save(buyerWallet);

        bike.setStatus(Bike.BikeStatus.RESERVED);
        bikeRepository.save(bike);

        savePointTransaction(buyerWallet.getUser(), bike.getPricePoints(),
                PointTransaction.TransactionType.ESCROW_HOLD, "OrderKey: " + idempotencyKey);

        Order order = new Order();
        order.setBuyer(buyerWallet.getUser());
        order.setBike(bike);
        order.setAmountPoints(bike.getPricePoints());
        order.setIdempotencyKey(idempotencyKey);
        order.setStatus(Order.OrderStatus.ESCROWED);

        Order saved = orderRepository.save(order);
        historyService.log("order", saved.getId(), "created", buyerId, null);
        historyService.log("bike", bike.getId(), "reserved", buyerId, null);
        return saved;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Order cancelOrder(Long orderId, Long buyerId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        assertBuyer(order, buyerId);
        assertStatus(order, Order.OrderStatus.ESCROWED);

        refundToBuyer(order, "Cancel Order: " + orderId);

        Bike bike = order.getBike();
        bike.setStatus(Bike.BikeStatus.ACTIVE);
        bikeRepository.save(bike);

        order.setStatus(Order.OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        historyService.log("order", saved.getId(), "cancelled", buyerId, null);
        historyService.log("bike", bike.getId(), "available", buyerId, null);
        return saved;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Order markDelivered(Long orderId, Long sellerId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        assertSeller(order, sellerId);
        assertStatus(order, Order.OrderStatus.ESCROWED);

        order.setStatus(Order.OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        historyService.log("order", saved.getId(), "delivered", sellerId, null);
        return saved;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Order confirmReceipt(Long orderId, Long buyerId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        assertBuyer(order, buyerId);
        assertStatus(order, Order.OrderStatus.DELIVERED);

        releaseToSeller(order, "Buyer confirmed receipt for Order: " + orderId);
        historyService.log("order", order.getId(), "confirmed_receipt", buyerId, null);
        return order;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Order requestReturn(Long orderId, Long buyerId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        assertBuyer(order, buyerId);
        assertStatus(order, Order.OrderStatus.DELIVERED);

        if (order.getDeliveredAt() == null || order.getDeliveredAt().plusDays(7).isBefore(LocalDateTime.now())) {
            throw new InvalidOrderStatusException("Return window of 7 days has expired");
        }

        order.setStatus(Order.OrderStatus.RETURN_REQUESTED);
        Order saved = orderRepository.save(order);
        historyService.log("order", saved.getId(), "return_requested", buyerId, null);
        return saved;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Order confirmReturn(Long orderId, Long sellerId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        assertSeller(order, sellerId);
        assertStatus(order, Order.OrderStatus.RETURN_REQUESTED);

        refundToBuyer(order, "Return confirmed for Order: " + orderId);

        Bike bike = order.getBike();
        bike.setStatus(Bike.BikeStatus.ACTIVE);
        bikeRepository.save(bike);

        order.setStatus(Order.OrderStatus.REFUNDED);
        Order saved = orderRepository.save(order);
        historyService.log("order", saved.getId(), "refunded", sellerId, null);
        historyService.log("bike", bike.getId(), "available", sellerId, null);
        return saved;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void releaseToSeller(Order order, String referenceLabel) {
        Long total = order.getAmountPoints();
        Long commission = (long) (total * 0.02);
        Long sellerRevenue = total - commission;

        UserWallet buyerWallet = walletRepository.findByUserIdForUpdate(order.getBuyer().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Buyer wallet not found"));
        UserWallet sellerWallet = walletRepository.findByUserIdForUpdate(order.getBike().getSeller().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller wallet not found"));

        buyerWallet.setFrozenPoints(buyerWallet.getFrozenPoints() - total);
        walletRepository.save(buyerWallet);

        sellerWallet.setAvailablePoints(sellerWallet.getAvailablePoints() + sellerRevenue);
        walletRepository.save(sellerWallet);

        savePointTransaction(sellerWallet.getUser(), sellerRevenue,
                PointTransaction.TransactionType.EARN, referenceLabel);

        order.setStatus(Order.OrderStatus.COMPLETED);
        Bike bike = order.getBike();
        bike.setStatus(Bike.BikeStatus.SOLD);
        bikeRepository.save(bike);
        orderRepository.save(order);
        historyService.log("order", order.getId(), "completed", order.getBike().getSeller().getId(), null);
    }

    public List<Order> findExpiredDeliveredOrders(LocalDateTime deadline) {
        return orderRepository.findExpiredDeliveredOrders(deadline);
    }

    // package-accessible so DisputeService can reuse without duplicating wallet logic
    void refundToBuyer(Order order, String referenceLabel) {
        Long amount = order.getAmountPoints();
        UserWallet buyerWallet = walletRepository.findByUserIdForUpdate(order.getBuyer().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Buyer wallet not found"));
        buyerWallet.setFrozenPoints(buyerWallet.getFrozenPoints() - amount);
        buyerWallet.setAvailablePoints(buyerWallet.getAvailablePoints() + amount);
        walletRepository.save(buyerWallet);
        savePointTransaction(buyerWallet.getUser(), amount,
                PointTransaction.TransactionType.ESCROW_RELEASE, referenceLabel);
    }

    private void savePointTransaction(User user, Long amount,
                                      PointTransaction.TransactionType type, String referenceId) {
        PointTransaction tx = new PointTransaction();
        tx.setUser(user);
        tx.setAmount(amount);
        tx.setType(type);
        tx.setStatus(PointTransaction.TransactionStatus.SUCCESS);
        tx.setReferenceId(referenceId);
        pointTxRepo.save(tx);
    }

    private void assertBuyer(Order order, Long userId) {
        if (!order.getBuyer().getId().equals(userId)) {
            throw new IllegalArgumentException("Only the buyer can perform this action");
        }
    }

    private void assertSeller(Order order, Long userId) {
        if (!order.getBike().getSeller().getId().equals(userId)) {
            throw new IllegalArgumentException("Only the seller can perform this action");
        }
    }

    private void assertStatus(Order order, Order.OrderStatus expected) {
        if (order.getStatus() != expected) {
            throw new InvalidOrderStatusException(
                    "Expected status " + expected + " but was " + order.getStatus());
        }
    }
}
