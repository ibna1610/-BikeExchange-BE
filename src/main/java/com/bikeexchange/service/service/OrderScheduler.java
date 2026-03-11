package com.bikeexchange.service.service;

import com.bikeexchange.model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderScheduler {

    @Autowired
    private OrderService orderService;

    /**
     * Chạy mỗi giờ.
     * Tìm các order ở trạng thái DELIVERED mà deliveredAt đã quá 7 ngày
     * và buyer chưa confirm receipt và không có return request
     * → tự động giải phóng điểm về seller.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void autoReleaseExpiredOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(7);
        List<Order> expiredOrders = orderService.findExpiredDeliveredOrders(deadline);

        for (Order order : expiredOrders) {
            try {
                orderService.releaseToSeller(order, "Auto-release after 7 days for Order: " + order.getId());
            } catch (Exception e) {
                // Log và tiếp tục với order tiếp theo
                System.err.println("Auto-release failed for order " + order.getId() + ": " + e.getMessage());
            }
        }
    }
}
