package com.bikeexchange.service;

import com.bikeexchange.model.*;
import com.bikeexchange.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AdminService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BikeRepository bikeRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private InspectionRepository inspectionRepository;

    @Autowired
    private ReportRepository reportRepository;

    // ==================== LISTING MANAGEMENT ====================
    public Bike approveListing(Long bikeId) {
        Bike bike = bikeRepository.findById(bikeId)
                .orElseThrow(() -> new RuntimeException("Bike not found"));

        // Chỉ có thể duyệt xe ở trạng thái AVAILABLE (chưa được duyệt)
        bike.setStatus(Bike.BikeStatus.AVAILABLE);
        return bikeRepository.save(bike);
    }

    public Bike rejectListing(Long bikeId, String reason) {
        Bike bike = bikeRepository.findById(bikeId)
                .orElseThrow(() -> new RuntimeException("Bike not found"));

        // Lưu lý do từ chối vào description tạm thời
        bike.setDescription("REJECTED: " + reason + "\nOriginal: " + bike.getDescription());
        bike.setStatus(Bike.BikeStatus.ARCHIVED); // Ẩn tin
        return bikeRepository.save(bike);
    }

    public Page<Bike> getPendingListings(Pageable pageable) {
        // TODO: Cần thêm trạng thái PENDING vào BikeStatus nếu cần
        // Tạm thời trả về xe được tạo trong 7 ngày gần nhất
        return bikeRepository.findByStatus(Bike.BikeStatus.AVAILABLE, pageable);
    }

    public Bike lockListing(Long bikeId) {
        Bike bike = bikeRepository.findById(bikeId)
                .orElseThrow(() -> new RuntimeException("Bike not found"));
        // TODO: Cần thêm trạng thái LOCKED vào BikeStatus
        return bike;
    }

    public void deleteListing(Long bikeId) {
        Bike bike = bikeRepository.findById(bikeId)
                .orElseThrow(() -> new RuntimeException("Bike not found"));
        bike.setStatus(Bike.BikeStatus.ARCHIVED);
        bikeRepository.save(bike);
    }

    // ==================== USER MANAGEMENT ====================
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Page<User> getUsersByRole(User.UserRole role, Pageable pageable) {
        // TODO: Cần thêm method trong UserRepository
        return userRepository.findAll(pageable);
    }

    public User activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsVerified(true);
        return userRepository.save(user);
    }

    public User deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsVerified(false);
        return userRepository.save(user);
    }

    public User suspendUser(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        // TODO: Cần thêm trường suspendedReason hoặc status = SUSPENDED vào User model
        user.setIsVerified(false);
        return userRepository.save(user);
    }

    public List<User> getUsersByEmail(String email) {
        return userRepository.findByEmailContaining(email);
    }

    // ==================== REPORT & DISPUTE HANDLING ====================
    public Page<Report> getPendingReports(Pageable pageable) {
        // TODO: Cần thêm method trong ReportRepository
        return reportRepository.findAll(pageable);
    }

    public Report resolveReport(Long reportId, String adminNote, Report.ReportStatus resolution) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        report.setStatus(resolution);
        report.setAdminNote(adminNote);
        report.setResolvedAt(LocalDateTime.now());

        // Nếu báo cáo được phê duyệt, cần hành động (xóa tin, khóa user, v.v.)
        if (resolution == Report.ReportStatus.RESOLVED) {
            handleResolvedReport(report);
        }

        return reportRepository.save(report);
    }

    private void handleResolvedReport(Report report) {
        if (report.getBike() != null) {
            // Xóa tin bị báo cáo
            report.getBike().setStatus(Bike.BikeStatus.ARCHIVED);
            bikeRepository.save(report.getBike());
        }

        if (report.getReportedUser() != null) {
            // Có thể khóa user hoặc giảm uy tín
            // TODO: Implement suspension logic
        }
    }

    public List<Report> getReportsByType(Report.ReportType type) {
        return reportRepository.findByReportType(type);
    }

    public Long getTotalReportsCount() {
        return reportRepository.count();
    }

    public Long getPendingReportsCount() {
        return reportRepository.countByStatus(Report.ReportStatus.PENDING);
    }

    // ==================== TRANSACTION MANAGEMENT ====================
    public Page<Transaction> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }

    public Page<Transaction> getTransactionsByStatus(Transaction.TransactionStatus status, Pageable pageable) {
        // TODO: Cần thêm method trong TransactionRepository
        return transactionRepository.findAll(pageable);
    }

    public Transaction cancelTransaction(Long transactionId, String reason) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        transaction.setStatus(Transaction.TransactionStatus.CANCELLED);
        transaction.setSellerNote(reason);

        // Giải phóng xe
        Bike bike = transaction.getBike();
        bike.setStatus(Bike.BikeStatus.AVAILABLE);
        bikeRepository.save(bike);

        return transactionRepository.save(transaction);
    }

    // ==================== INSPECTION APPROVAL ====================
    public Inspection approveInspection(Long inspectionId) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new RuntimeException("Inspection not found"));

        inspection.setStatus(Inspection.InspectionStatus.APPROVED);
        inspection.setCompletedAt(LocalDateTime.now());
        // Báo cáo có hiệu lực 90 ngày
        inspection.setValidUntil(LocalDateTime.now().plusDays(90));

        return inspectionRepository.save(inspection);
    }

    public Inspection rejectInspection(Long inspectionId, String reason) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new RuntimeException("Inspection not found"));

        inspection.setStatus(Inspection.InspectionStatus.REJECTED);
        inspection.setReportDescription(reason);

        return inspectionRepository.save(inspection);
    }

    public Page<Inspection> getPendingInspections(Pageable pageable) {
        // TODO: Cần convert to Page properly
        return inspectionRepository.findAll(pageable);
    }

    // ==================== STATISTICS & DASHBOARD ====================
    public Map<String, Object> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Counting statistics
        metrics.put("totalUsers", userRepository.count());
        metrics.put("totalBikes", bikeRepository.count());
        metrics.put("totalTransactions", transactionRepository.count());
        metrics.put("pendingReports", reportRepository.countByStatus(Report.ReportStatus.PENDING));

        // Transaction statistics
        metrics.put("completedTransactions", transactionRepository.count()); // TODO: Filter by status
        metrics.put("cancelledTransactions", transactionRepository.count()); // TODO: Filter by status
        metrics.put("disputedTransactions", transactionRepository.count()); // TODO: Filter by status

        // Inspection statistics
        metrics.put("pendingInspections", inspectionRepository.findByStatus(Inspection.InspectionStatus.PENDING).size());
        metrics.put("approvedInspections", inspectionRepository.findByStatus(Inspection.InspectionStatus.APPROVED).size());

        return metrics;
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // User statistics
        stats.put("totalBuyers", userRepository.count()); // TODO: Filter by role
        stats.put("totalSellers", userRepository.count()); // TODO: Filter by role
        stats.put("totalInspectors", userRepository.count()); // TODO: Filter by role

        // Listing statistics
        stats.put("totalListings", bikeRepository.count());
        stats.put("activeListings", bikeRepository.findByStatus(Bike.BikeStatus.AVAILABLE, 
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getNumberOfElements());
        stats.put("soldListings", bikeRepository.findByStatus(Bike.BikeStatus.SOLD,
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getNumberOfElements());

        // Transaction revenue (approximation)
        stats.put("totalTransactionValue", transactionRepository.findAll().stream()
                .mapToLong(Transaction::getTransactionPrice)
                .sum());

        // Performance metrics
        stats.put("averageRating", userRepository.findAll().stream()
                .mapToDouble(User::getRating)
                .average()
                .orElse(0.0));

        return stats;
    }

    public Map<String, Object> getInspectionStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("pendingInspections", inspectionRepository.findByStatus(Inspection.InspectionStatus.PENDING).size());
        stats.put("approvedInspections", inspectionRepository.findByStatus(Inspection.InspectionStatus.APPROVED).size());
        stats.put("rejectedInspections", inspectionRepository.findByStatus(Inspection.InspectionStatus.REJECTED).size());

        return stats;
    }

    public Map<String, Object> getReportStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("pendingReports", reportRepository.countByStatus(Report.ReportStatus.PENDING));
        stats.put("reviewingReports", reportRepository.countByStatus(Report.ReportStatus.REVIEWING));
        stats.put("resolvedReports", reportRepository.countByStatus(Report.ReportStatus.RESOLVED));

        return stats;
    }
}
