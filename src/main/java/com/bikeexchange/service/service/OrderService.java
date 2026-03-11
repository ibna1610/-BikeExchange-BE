package com.bikeexchange.service.service;

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

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private UserWalletRepository walletRepository;
    @Autowired private BikeRepository bikeRepository;
    @Autowired private PointTransactionRepository pointTxRepo;
    @Autowired private HistoryService historyService;

    // ─── Create ────────────────────────────────────────────────────────────────
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

    // ─── Cancel (buyer hủy trước khi giao) ──────────────────────────────────────────
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

    // ─── Deliver (seller đánh dấu đã giao) ──────────────────────────────────────────
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

    // ─── Confirm receipt (buyer xác nhận → tiền về seller ngay) ─────────────────────
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

    // ─── Request return (buyer hoàn hàng trong 7 ngày từ deliveredAt) ──────────────
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

    // ─── Confirm return (seller xác nhận nhận lại hàng → hoàn điểm về buyer) ─────────
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

    // ─── Release to seller (buyer confirm hoặc scheduler auto sau 7 ngày) ───────────
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

    // ─── Scheduler helper ──────────────────────────────────────────────────────────
    public List<Order> findExpiredDeliveredOrders(LocalDateTime deadline) {
        return orderRepository.findExpiredDeliveredOrders(deadline);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────────
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

        Order saved = orderRepository.save(order);
        historyService.log("order", saved.getId(), "created", buyerId, null);
        historyService.log("bike", bike.getId(), "reserved", buyerId, null);
        return saved;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Order cancelOrder(Long orderId, Long buyerId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order missing"));

        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new IllegalArgumentException("Only buyer can cancel");
        }

        if (order.getStatus() != Order.OrderStatus.ESCROWED) {
            throw new InvalidOrderStatusException("Order is not in ESCROWED state");
        }

        Long amount = order.getAmountPoints();

        UserWallet buyerWallet = walletRepository.findByUserIdForUpdate(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer wallet missing"));

        // Release frozen points back to available
        buyerWallet.setFrozenPoints(buyerWallet.getFrozenPoints() - amount);
        buyerWallet.setAvailablePoints(buyerWallet.getAvailablePoints() + amount);
        walletRepository.save(buyerWallet);

        // Log transaction
        PointTransaction pTx = new PointTransaction();
        pTx.setUser(buyerWallet.getUser());
        pTx.setAmount(amount);
        pTx.setType(PointTransaction.TransactionType.ESCROW_RELEASE);
        pTx.setStatus(PointTransaction.TransactionStatus.SUCCESS);
        pTx.setReferenceId("Cancel Order: " + orderId);
        pointTxRepo.save(pTx);

        order.setStatus(Order.OrderStatus.CANCELLED);

        Bike listing = order.getBike();
        listing.setStatus(Bike.BikeStatus.ACTIVE); // make available again
        bikeRepository.save(listing);

        Order saved = orderRepository.save(order);
        historyService.log("order", saved.getId(), "cancelled", buyerId, null);
        historyService.log("bike", listing.getId(), "available", buyerId, null);
        return saved;
    }

    // ─── Seller đánh dấu đã giao hàng ───────────────────────────────────────────
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Order markDelivered(Long orderId, Long sellerId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order missing"));

        if (!order.getBike().getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("Only seller can mark as delivered");
        }
        if (order.getStatus() != Order.OrderStatus.ESCROWED) {
            throw new InvalidOrderStatusException("Order must be in ESCROWED state to mark as delivered");
        }

        order.setStatus(Order.OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        historyService.log("order", saved.getId(), "delivered", sellerId, null);
        return saved;
    }

    // ─── Buyer xác nhận đã nhận hàng → tiền về seller ngay lập tức ───────────────
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Order confirmReceipt(Long orderId, Long buyerId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order missing"));

        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new IllegalArgumentException("Only buyer can confirm receipt");
        }
        if (order.getStatus() != Order.OrderStatus.DELIVERED) {
            throw new InvalidOrderStatusException("Order must be in DELIVERED state");
        }

        // Giải phóng tiền về seller ngay khi buyer xác nhận nhận hàng
        releaseToSeller(order, "Buyer confirmed receipt for Order: " + orderId);
        historyService.log("order", order.getId(), "confirmed_receipt", buyerId, null);
        return order;
    }

    // ─── Buyer yêu cầu hoàn hàng (chỉ trong 7 ngày kể từ deliveredAt) ──────────
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Order requestReturn(Long orderId, Long buyerId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order missing"));

        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new IllegalArgumentException("Only buyer can request return");
        }
        if (order.getStatus() != Order.OrderStatus.DELIVERED) {
            throw new InvalidOrderStatusException("Order must be in DELIVERED state to request return");
        }
        if (order.getDeliveredAt() == null || order.getDeliveredAt().plusDays(7).isBefore(LocalDateTime.now())) {
            throw new InvalidOrderStatusException("Return window of 7 days from delivery date has expired");
        }

        order.setStatus(Order.OrderStatus.RETURN_REQUESTED);
        Order saved = orderRepository.save(order);
        historyService.log("order", saved.getId(), "return_requested", buyerId, null);
        return saved;
    }

    // ─── Seller xác nhận đã nhận lại hàng → hoàn điểm cho buyer ──────────────────
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Order confirmReturn(Long orderId, Long sellerId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order missing"));

        if (!order.getBike().getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("Only seller can confirm return");
        }
        if (order.getStatus() != Order.OrderStatus.RETURN_REQUESTED) {
            throw new InvalidOrderStatusException("Order must be in RETURN_REQUESTED state");
        }

        Long amount = order.getAmountPoints();
        UserWallet buyerWallet = walletRepository.findByUserIdForUpdate(order.getBuyer().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Buyer wallet missing"));

        // Hoàn điểm: frozen → available của buyer
        buyerWallet.setFrozenPoints(buyerWallet.getFrozenPoints() - amount);
        buyerWallet.setAvailablePoints(buyerWallet.getAvailablePoints() + amount);
        walletRepository.save(buyerWallet);

        PointTransaction pTx = new PointTransaction();
        pTx.setUser(buyerWallet.getUser());
        pTx.setAmount(amount);
        pTx.setType(PointTransaction.TransactionType.ESCROW_RELEASE);
        pTx.setStatus(PointTransaction.TransactionStatus.SUCCESS);
        pTx.setReferenceId("Return confirmed for Order: " + orderId);
        pointTxRepo.save(pTx);

        order.setStatus(Order.OrderStatus.RETURN_CONFIRMED);
        orderRepository.save(order);

        // Đặt lại trạng thái bike active để seller có thể đăng bán lại
        Bike bike = order.getBike();
        bike.setStatus(Bike.BikeStatus.ACTIVE);
        bikeRepository.save(bike);

        order.setStatus(Order.OrderStatus.REFUNDED);
        Order saved = orderRepository.save(order);
        historyService.log("order", saved.getId(), "refunded", sellerId, null);
        historyService.log("bike", bike.getId(), "available", sellerId, null);
        return saved;
    }

    // ─── Giải phóng điểm về seller (dùng bởi confirm receipt và scheduler) ─────────────
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void releaseToSeller(Order order, String referenceLabel) {
        Long total = order.getAmountPoints();
        Long adminCommission = (long) (total * 0.02);
        Long sellerRevenue = total - adminCommission;

        UserWallet buyerWallet = walletRepository.findByUserIdForUpdate(order.getBuyer().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Buyer wallet missing"));
        UserWallet sellerWallet = walletRepository.findByUserIdForUpdate(order.getBike().getSeller().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller wallet missing"));

        buyerWallet.setFrozenPoints(buyerWallet.getFrozenPoints() - total);
        walletRepository.save(buyerWallet);

        sellerWallet.setAvailablePoints(sellerWallet.getAvailablePoints() + sellerRevenue);
        walletRepository.save(sellerWallet);

        PointTransaction pTx = new PointTransaction();
        pTx.setUser(sellerWallet.getUser());
        pTx.setAmount(sellerRevenue);
        pTx.setType(PointTransaction.TransactionType.EARN);
        pTx.setStatus(PointTransaction.TransactionStatus.SUCCESS);
        pTx.setReferenceId(referenceLabel);
        pointTxRepo.save(pTx);

        order.setStatus(Order.OrderStatus.COMPLETED);
        Bike listing = order.getBike();
        listing.setStatus(Bike.BikeStatus.SOLD);
        bikeRepository.save(listing);
        orderRepository.save(order);
        historyService.log("order", order.getId(), "completed", order.getBike().getSeller().getId(), null);
    }

    public List<Order> findExpiredDeliveredOrders(LocalDateTime deadline) {
        return orderRepository.findExpiredDeliveredOrders(deadline);
    }
}
