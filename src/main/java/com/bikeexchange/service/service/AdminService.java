package com.bikeexchange.service.service;

import com.bikeexchange.dto.response.*;
import com.bikeexchange.model.*;
import com.bikeexchange.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private PointTransactionRepository pointTransactionRepository;

    @Autowired
    private UserWalletRepository userWalletRepository;

    @Autowired
    private com.bikeexchange.repository.UserReportRepository userReportRepository;

    @Autowired
    private OrderRuleConfigService orderRuleConfigService;

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

    public SystemWalletSummaryResponse getSystemWalletSummary() {
        SystemWalletSummaryResponse summary = new SystemWalletSummaryResponse();

        // 1. Point Summary from Wallets
        Long totalAvailable = userWalletRepository.sumTotalAvailablePoints();
        Long totalFrozen = userWalletRepository.sumTotalFrozenPoints();
        summary.setTotalSystemAvailablePoints(totalAvailable != null ? totalAvailable : 0L);
        summary.setTotalSystemFrozenPoints(totalFrozen != null ? totalFrozen : 0L);

        // 2. Escrow Orders (Orders currently holding money)
        List<Order.OrderStatus> escrowStatuses = List.of(
            Order.OrderStatus.ESCROWED,
            Order.OrderStatus.ACCEPTED,
            Order.OrderStatus.SHIPPED,
            Order.OrderStatus.DELIVERED,
            Order.OrderStatus.RETURN_REQUESTED,
            Order.OrderStatus.DISPUTED
        );
        List<Order> escrowOrders = orderRepository.findByStatusIn(escrowStatuses);
        Long totalEscrowPoints = orderRepository.sumAmountByStatusIn(escrowStatuses);
        
        summary.setEscrowOrdersCount(escrowOrders.size());
        summary.setTotalEscrowPoints(totalEscrowPoints != null ? totalEscrowPoints : 0L);
        summary.setEscrowOrders(escrowOrders.stream()
            .map(OrderResponse::fromEntity)
            .collect(Collectors.toList()));

        // 3. Pending Withdrawals (Transactions currently holding money)
        List<PointTransaction> pendingWithdrawals = pointTransactionRepository.findByTypeAndStatusOrderByCreatedAtDesc(
            PointTransaction.TransactionType.WITHDRAW,
            PointTransaction.TransactionStatus.PENDING
        );
        Long totalPendingWithdrawals = pointTransactionRepository.sumAmountByTypeAndStatus(
            PointTransaction.TransactionType.WITHDRAW,
            PointTransaction.TransactionStatus.PENDING
        );

        summary.setPendingWithdrawalsCount(pendingWithdrawals.size());
        summary.setTotalPendingWithdrawalPoints(totalPendingWithdrawals != null ? totalPendingWithdrawals : 0L);
        summary.setPendingWithdrawals(pendingWithdrawals.stream()
            .map(PointTransactionDto::from)
            .collect(Collectors.toList()));

        // 4. Inspection Summary (Sellers holding money for inspection)
        List<InspectionRequest.RequestStatus> activeInspectionStatuses = List.of(
            InspectionRequest.RequestStatus.REQUESTED,
            InspectionRequest.RequestStatus.ASSIGNED,
            InspectionRequest.RequestStatus.IN_PROGRESS,
            InspectionRequest.RequestStatus.INSPECTED
        );
        List<InspectionRequest> activeInspections = inspectionRepository.findByStatusIn(activeInspectionStatuses);
        Long totalInspectionFeePoints = inspectionRepository.sumFeePointsByStatusIn(activeInspectionStatuses);

        summary.setActiveInspectionsCount(activeInspections.size());
        summary.setTotalInspectionFeePoints(totalInspectionFeePoints != null ? totalInspectionFeePoints : 0L);
        summary.setActiveInspections(activeInspections);

        // 4. Verification Logic
        long calculatedTotalFrozen = (totalEscrowPoints != null ? totalEscrowPoints : 0L) + 
                                   (totalPendingWithdrawals != null ? totalPendingWithdrawals : 0L) +
                                   (totalInspectionFeePoints != null ? totalInspectionFeePoints : 0L);
        long actualSystemFrozen = summary.getTotalSystemFrozenPoints();
        
        summary.setIsBalanced(calculatedTotalFrozen == actualSystemFrozen);
        if (summary.getIsBalanced()) {
            summary.setBalanceCheckMessage("Số tiền hoàn toàn trùng khớp: Tổng đóng băng = Tiền Escrow + Rút Tiền Đang Xử Lý + Phí Kiểm Định (" + actualSystemFrozen + " points)");
        } else {
            summary.setBalanceCheckMessage("CẢNH BÁO: Số tiền LỆCH - Tổng ví đóng băng (" + actualSystemFrozen + 
                                         ") != [Escrow (" + (totalEscrowPoints != null ? totalEscrowPoints : 0L) + 
                                         ") + Pending Withdraw (" + (totalPendingWithdrawals != null ? totalPendingWithdrawals : 0L) + 
                                         ") + Inspection Fee (" + (totalInspectionFeePoints != null ? totalInspectionFeePoints : 0L) + ")]");
        }

        return summary;
    }

    public AdminRevenueSummaryResponse getSystemRevenueSummary() {
        AdminRevenueSummaryResponse res = new AdminRevenueSummaryResponse();

        // Get all successful SPEND transactions to categorize
        List<PointTransaction> allSpend = pointTransactionRepository.findByTypeAndStatusOrderByCreatedAtDesc(
                PointTransaction.TransactionType.SPEND, PointTransaction.TransactionStatus.SUCCESS);
        
        // 1. Phí đăng tin xe lẻ (BIKE_POST_FEE_)
        List<PointTransaction> postingFees = allSpend.stream()
                .filter(tx -> tx.getReferenceId() != null && tx.getReferenceId().startsWith("BIKE_POST_FEE_"))
                .collect(Collectors.toList());
        long postingTotal = postingFees.stream().mapToLong(PointTransaction::getAmount).sum();
        res.setPostingFeesCount(postingFees.size());
        res.setPostingFeesTotal(postingTotal);
        res.setPostingFeeDetails(postingFees.stream().map(PointTransactionDto::from).collect(Collectors.toList()));

        // 2. Phí mua gói Combo (BUY_COMBO_)
        List<PointTransaction> comboFees = allSpend.stream()
                .filter(tx -> tx.getReferenceId() != null && tx.getReferenceId().startsWith("BUY_COMBO_"))
                .collect(Collectors.toList());
        long comboTotal = comboFees.stream().mapToLong(PointTransaction::getAmount).sum();
        res.setComboFeesCount(comboFees.size());
        res.setComboFeesTotal(comboTotal);
        res.setComboFeeDetails(comboFees.stream().map(PointTransactionDto::from).collect(Collectors.toList()));

        // 3. Phí nâng cấp Seller (Seller Upgrade Fee)
        List<PointTransaction> upgradeFees = allSpend.stream()
                .filter(tx -> "Seller Upgrade Fee".equals(tx.getReferenceId()))
                .collect(Collectors.toList());
        long upgradeTotal = upgradeFees.stream().mapToLong(PointTransaction::getAmount).sum();
        res.setUpgradeFeesCount(upgradeFees.size());
        res.setUpgradeFeesTotal(upgradeTotal);
        res.setUpgradeFeeDetails(upgradeFees.stream().map(PointTransactionDto::from).collect(Collectors.toList()));

        // 4. Phí kiểm định hệ thống thực thu (APPROVED Inspections)
        List<InspectionRequest> approvedInspections = inspectionRepository.findByStatusIn(List.of(InspectionRequest.RequestStatus.APPROVED));
        long inspectionTotal = approvedInspections.stream().mapToLong(InspectionRequest::getFeePoints).sum();
        res.setInspectionFeesCount(approvedInspections.size());
        res.setInspectionFeesTotal(inspectionTotal);
        res.setInspectionFeeDetails(approvedInspections.stream()
            .map(InspectionResponse::fromEntity)
            .collect(Collectors.toList()));

        // 5. Hoa hồng từ đơn hàng (COMPLETED orders)
        List<Order> completedOrders = orderRepository.findByStatusIn(List.of(Order.OrderStatus.COMPLETED));
        double currentCommissionRate = orderRuleConfigService.getCommissionRate();
        
        long commissionTotal = 0;
        List<PointTransactionDto> commissionDetails = new ArrayList<>();
        
        for (Order o : completedOrders) {
            long commAmount = Math.round(o.getAmountPoints() * currentCommissionRate);
            commissionTotal += commAmount;
            
            PointTransactionDto dto = new PointTransactionDto();
            dto.setAmount(commAmount);
            dto.setType(PointTransaction.TransactionType.COMMISSION.name());
            dto.setStatus(PointTransaction.TransactionStatus.SUCCESS.name());
            dto.setRemarks("Hoa hồng từ đơn hàng #" + o.getId() + " (" + o.getBike().getTitle() + ")");
            dto.setCreatedAt(o.getUpdatedAt());
            commissionDetails.add(dto);
        }
        res.setOrderCommissionCount(completedOrders.size());
        res.setOrderCommissionTotal(commissionTotal);
        res.setCommissionDetails(commissionDetails);

        res.setTotalRevenue(postingTotal + comboTotal + upgradeTotal + inspectionTotal + commissionTotal);
        
        return res;
    }
}
