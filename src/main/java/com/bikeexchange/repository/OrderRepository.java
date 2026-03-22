package com.bikeexchange.repository;

import com.bikeexchange.model.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    boolean existsByShippingCarrierIgnoreCaseAndTrackingCodeIgnoreCase(String shippingCarrier, String trackingCode);

    List<Order> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    List<Order> findByBuyerIdAndStatusInOrderByCreatedAtDesc(Long buyerId, List<Order.OrderStatus> statuses);

    List<Order> findByBikeSellerIdOrderByCreatedAtDesc(Long sellerId);

    List<Order> findByBikeSellerIdAndStatusInOrderByCreatedAtDesc(Long sellerId, List<Order.OrderStatus> statuses);

    // Tìm các order đang DELIVERED và deliveredAt đã quá 14 ngày (dùng cho scheduler auto-release)
    @Query("SELECT o FROM Order o WHERE o.status = 'DELIVERED' AND o.deliveredAt < :deadline")
    List<Order> findExpiredDeliveredOrders(@Param("deadline") LocalDateTime deadline);
}
