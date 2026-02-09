package com.bikeexchange.service;

import com.bikeexchange.model.*;
import com.bikeexchange.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BuyerService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BikeRepository bikeRepository;

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private InspectionRepository inspectionRepository;

    @Autowired
    private ReportRepository reportRepository;

    // ==================== WISHLIST ====================
    public Wishlist addToWishlist(Long buyerId, Long bikeId) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));
        Bike bike = bikeRepository.findById(bikeId)
                .orElseThrow(() -> new RuntimeException("Bike not found"));

        // Kiểm tra xe đã trong wishlist chưa
        if (wishlistRepository.findByBuyerIdAndBikeId(buyerId, bikeId).isPresent()) {
            throw new IllegalArgumentException("Bike already in wishlist");
        }

        // Kiểm tra số lượng wishlist (giới hạn N xe)
        Long wishlistCount = wishlistRepository.countByBuyerId(buyerId);
        if (wishlistCount >= 50) { // Giới hạn 50 xe yêu thích
            throw new IllegalArgumentException("Wishlist limit reached (max 50)");
        }

        Wishlist wishlist = new Wishlist();
        wishlist.setBuyer(buyer);
        wishlist.setBike(bike);
        return wishlistRepository.save(wishlist);
    }

    public void removeFromWishlist(Long buyerId, Long bikeId) {
        wishlistRepository.deleteByBuyerIdAndBikeId(buyerId, bikeId);
    }

    public List<Wishlist> getWishlist(Long buyerId) {
        return wishlistRepository.findByBuyerId(buyerId);
    }

    public boolean isInWishlist(Long buyerId, Long bikeId) {
        return wishlistRepository.findByBuyerIdAndBikeId(buyerId, bikeId).isPresent();
    }

    // ==================== SEARCH & FILTER ====================
    /**
     * Tìm kiếm & lọc nâng cao cho Buyer
     */
    public Page<Bike> searchBikesAdvanced(Long minPrice, Long maxPrice, String brand, 
                                          String bikeType, Integer minYear, 
                                          String condition, Pageable pageable) {
        // TODO: Cần cập nhật BikeRepository với @Query phức tạp hơn
        // Tạm thời sử dụng filter cơ bản
        return bikeRepository.filterBikes(minPrice, maxPrice, minYear, pageable);
    }

    public Bike viewBikeDetail(Long bikeId) {
        Bike bike = bikeRepository.findById(bikeId)
                .orElseThrow(() -> new RuntimeException("Bike not found"));
        
        // Tăng view count
        bike.setViews(bike.getViews() + 1);
        return bikeRepository.save(bike);
    }

    // ==================== INSPECTION ====================
    public Inspection requestInspection(Long buyerId, Long bikeId, Long inspectionFee) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));
        Bike bike = bikeRepository.findById(bikeId)
                .orElseThrow(() -> new RuntimeException("Bike not found"));

        Inspection inspection = new Inspection();
        inspection.setBike(bike);
        inspection.setRequester(buyer);
        inspection.setInspectionFee(inspectionFee);
        inspection.setStatus(Inspection.InspectionStatus.PENDING);
        inspection.setIsPaid(false);

        return inspectionRepository.save(inspection);
    }

    public Optional<Inspection> getLatestInspectionApproved(Long bikeId) {
        return inspectionRepository.findFirstByBikeIdAndStatusOrderByCreatedAtDesc(
                bikeId, Inspection.InspectionStatus.APPROVED);
    }

    public List<Inspection> getInspectionsByBike(Long bikeId) {
        return inspectionRepository.findByBikeId(bikeId);
    }

    // ==================== PURCHASE & TRANSACTION ====================
    public Transaction makePurchase(Long buyerId, Long bikeId, Long depositAmount) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));
        Bike bike = bikeRepository.findById(bikeId)
                .orElseThrow(() -> new RuntimeException("Bike not found"));

        // Kiểm tra xe không phải RESERVED hoặc SOLD
        if (bike.getStatus() != Bike.BikeStatus.AVAILABLE) {
            throw new IllegalArgumentException("Bike is not available for purchase");
        }

        // Kiểm tra chỉ có một giao dịch đang xử lý cho xe này
        List<Transaction> activeTransactions = transactionRepository
                .findByBikeIdAndStatus(bikeId, Transaction.TransactionStatus.PENDING);
        if (!activeTransactions.isEmpty()) {
            throw new IllegalArgumentException("Another buyer has already put a deposit on this bike");
        }

        Transaction transaction = new Transaction();
        transaction.setBuyer(buyer);
        transaction.setSeller(bike.getSeller());
        transaction.setBike(bike);
        transaction.setTransactionPrice(depositAmount);
        transaction.setStatus(Transaction.TransactionStatus.PENDING);

        // Đánh dấu xe là RESERVED
        bike.setStatus(Bike.BikeStatus.RESERVED);
        bikeRepository.save(bike);

        return transactionRepository.save(transaction);
    }

    public Transaction trackTransaction(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }

    public List<Transaction> getBuyerTransactions(Long buyerId) {
        return transactionRepository.findByBuyerId(buyerId);
    }

    public Transaction completeTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());

        // Đánh dấu xe là SOLD
        Bike bike = transaction.getBike();
        bike.setStatus(Bike.BikeStatus.SOLD);
        bikeRepository.save(bike);

        // Tăng số lượng xe bán cho Seller
        User seller = transaction.getSeller();
        seller.setTotalBikesSold(seller.getTotalBikesSold() + 1);
        userRepository.save(seller);

        return transactionRepository.save(transaction);
    }

    // ==================== RATING & REVIEW ====================
    public Transaction rateTransaction(Long transactionId, Double rating, String review) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // Kiểm tra giao dịch đã hoàn tất
        if (transaction.getStatus() != Transaction.TransactionStatus.COMPLETED) {
            throw new IllegalArgumentException("Can only rate completed transactions");
        }

        // Kiểm tra chưa đánh giá
        if (transaction.getBuyerRating() != null) {
            throw new IllegalArgumentException("Transaction already rated by buyer");
        }

        transaction.setBuyerRating(rating);
        transaction.setBuyerReview(review);

        // Cập nhật điểm uy tín của Seller
        User seller = transaction.getSeller();
        updateSellerRating(seller.getId());

        return transactionRepository.save(transaction);
    }

    private void updateSellerRating(Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        List<Transaction> completedTransactions = transactionRepository
                .findBySellerIdAndStatus(sellerId, Transaction.TransactionStatus.COMPLETED);

        if (completedTransactions.isEmpty()) {
            seller.setRating(5.0);
        } else {
            Double averageRating = completedTransactions.stream()
                    .filter(t -> t.getBuyerRating() != null)
                    .mapToDouble(Transaction::getBuyerRating)
                    .average()
                    .orElse(5.0);
            seller.setRating(averageRating);
        }

        userRepository.save(seller);
    }

    // ==================== REPORT ====================
    public Report submitReport(Long reporterId, Long bikeId, Long reportedUserId,
                              Report.ReportType reportType, String description) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("Reporter not found"));

        Report report = new Report();
        report.setReporter(reporter);
        report.setReportType(reportType);
        report.setDescription(description);
        report.setStatus(Report.ReportStatus.PENDING);

        if (bikeId != null) {
            Bike bike = bikeRepository.findById(bikeId)
                    .orElseThrow(() -> new RuntimeException("Bike not found"));
            report.setBike(bike);
        }

        if (reportedUserId != null) {
            User reportedUser = userRepository.findById(reportedUserId)
                    .orElseThrow(() -> new RuntimeException("Reported user not found"));
            report.setReportedUser(reportedUser);
        }

        return reportRepository.save(report);
    }

    public List<Report> getMyReports(Long buyerId) {
        return reportRepository.findByReporterId(buyerId);
    }

    // ==================== REFUND & RETURN ====================
    public Transaction cancelPurchase(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // Chỉ có thể hủy khi PENDING
        if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
            throw new IllegalArgumentException("Can only cancel pending transactions");
        }

        transaction.setStatus(Transaction.TransactionStatus.CANCELLED);

        // Giải phóng xe từ RESERVED
        Bike bike = transaction.getBike();
        bike.setStatus(Bike.BikeStatus.AVAILABLE);
        bikeRepository.save(bike);

        return transactionRepository.save(transaction);
    }

    public Transaction disputeTransaction(Long transactionId, String reason) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        transaction.setStatus(Transaction.TransactionStatus.DISPUTED);
        transaction.setBuyerNote(reason);

        return transactionRepository.save(transaction);
    }
}
