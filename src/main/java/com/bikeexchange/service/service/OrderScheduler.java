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

    @Autowired
    private OrderRuleConfigService orderRuleConfigService;

    /**
     * Chạy mỗi giờ.
     * Tìm các order ở trạng thái DELIVERED mà deliveredAt đã quá 14 ngày
     * và buyer chưa confirm receipt và không có return request
     * → tự động giải phóng điểm về seller.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void autoReleaseExpiredOrders() {
        int returnWindowDays = orderRuleConfigService.getReturnWindowDays();
        LocalDateTime deadline = LocalDateTime.now().minusDays(returnWindowDays);
        List<Order> expiredOrders = orderService.findExpiredDeliveredOrders(deadline);

        for (Order order : expiredOrders) {
            try {
                orderService.releaseToSeller(order, "Auto-release after 14 days for Order: " + order.getId());
            } catch (Exception e) {
                // Log và tiếp tục với order tiếp theo
                System.err.println("Auto-release failed for order " + order.getId() + ": " + e.getMessage());
            }
        }
    }
}
