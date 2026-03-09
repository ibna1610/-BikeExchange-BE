package com.bikeexchange.service.service;

import com.bikeexchange.model.*;
import com.bikeexchange.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BikeRepository bikeRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DisputeRepository disputeRepository;

    @Autowired
    private InspectionRepository inspectionRepository;

    @Autowired
    private com.bikeexchange.repository.UserReportRepository userReportRepository;

    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // 1. Total Users
        long totalUsers = userRepository.count();
        metrics.put("totalUsers", totalUsers);

        // 2. Total Listings
        long totalListings = bikeRepository.count();
        metrics.put("totalListings", totalListings);

        // 3. Completed Orders and Revenue
        List<Order> allOrders = orderRepository.findAll();
        long completedOrders = 0;
        long totalCommissionRevenue = 0;

        for (Order order : allOrders) {
            if (order.getStatus() == Order.OrderStatus.COMPLETED) {
                completedOrders++;
                totalCommissionRevenue += (long) (order.getAmountPoints() * 0.05);
            }
        }
        metrics.put("totalCompletedOrders", completedOrders);
        metrics.put("totalCommissionRevenue", totalCommissionRevenue);

        // 4. Pending Disputes
        long pendingDisputes = disputeRepository.findByStatus(Dispute.DisputeStatus.OPEN).size();
        metrics.put("pendingDisputes", pendingDisputes);

        // 5. Inspection Statistics
        List<InspectionRequest> allInspections = inspectionRepository.findAll();
        long pendingInspections = allInspections.stream()
                .filter(i -> i.getStatus() == InspectionRequest.RequestStatus.REQUESTED).count();
        long assignedInspections = allInspections.stream()
                .filter(i -> i.getStatus() == InspectionRequest.RequestStatus.ASSIGNED).count();
        long inspectedInspections = allInspections.stream()
                .filter(i -> i.getStatus() == InspectionRequest.RequestStatus.INSPECTED).count();

        Map<String, Long> inspectionStats = new HashMap<>();
        inspectionStats.put("requested", pendingInspections);
        inspectionStats.put("assigned", assignedInspections);
        inspectionStats.put("inspectedWaitApprove", inspectedInspections);
        metrics.put("inspectionStatistics", inspectionStats);

        return metrics;
    }

    public Map<String, Long> getInspectionMetrics() {
        Map<String, Long> inspectionStats = new HashMap<>();
        List<InspectionRequest> allInspections = inspectionRepository.findAll();
        inspectionStats.put("requested", allInspections.stream()
                .filter(i -> i.getStatus() == InspectionRequest.RequestStatus.REQUESTED).count());
        inspectionStats.put("assigned", allInspections.stream()
                .filter(i -> i.getStatus() == InspectionRequest.RequestStatus.ASSIGNED).count());
        inspectionStats.put("inspected", allInspections.stream()
                .filter(i -> i.getStatus() == InspectionRequest.RequestStatus.INSPECTED).count());
        return inspectionStats;
    }

    public Map<String, Long> getReportMetrics() {
        Map<String, Long> reportStats = new HashMap<>();
        long total = userReportRepository.count();
        long pending = userReportRepository.countByStatus(com.bikeexchange.model.Report.ReportStatus.PENDING);
        reportStats.put("total", total);
        reportStats.put("pending", pending);
        return reportStats;
    }

    public long getReportsCount() {
        return userReportRepository.count();
    }

    public long getPendingReportsCount() {
        return userReportRepository.countByStatus(com.bikeexchange.model.Report.ReportStatus.PENDING);
    }
}
