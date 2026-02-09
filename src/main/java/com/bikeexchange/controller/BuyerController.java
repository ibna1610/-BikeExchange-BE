package com.bikeexchange.controller;

import com.bikeexchange.model.*;
import com.bikeexchange.service.BuyerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * Buyer API - Chức năng cho người mua
 * Xem danh sách xe, tìm kiếm, lọc, yêu cầu kiểm định, đặt mua, đánh giá, báo cáo
 */
@RestController
@RequestMapping("/api/buyer")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Buyer Management", description = "API cho chức năng người mua (Buyer)")
public class BuyerController {
    @Autowired
    private BuyerService buyerService;

    // ==================== WISHLIST ====================
    @PostMapping("/{buyerId}/wishlist/add/{bikeId}")
    @Operation(summary = "Thêm xe vào danh sách yêu thích",
            description = "Cho phép Buyer lưu xe vào wishlist (giới hạn 50 xe)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Thêm thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi - Xe đã có trong wishlist hoặc vượt giới hạn")
    })
    public ResponseEntity<?> addToWishlist(
            @Parameter(description = "ID của Buyer") @PathVariable Long buyerId,
            @Parameter(description = "ID của Bike") @PathVariable Long bikeId) {
        try {
            Wishlist wishlist = buyerService.addToWishlist(buyerId, bikeId);
            return ResponseEntity.status(HttpStatus.CREATED).body(wishlist);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{buyerId}/wishlist/remove/{bikeId}")
    @Operation(summary = "Xóa xe khỏi wishlist")
    @ApiResponse(responseCode = "200", description = "Xóa thành công")
    public ResponseEntity<?> removeFromWishlist(
            @Parameter(description = "ID của Buyer") @PathVariable Long buyerId,
            @Parameter(description = "ID của Bike") @PathVariable Long bikeId) {
        try {
            buyerService.removeFromWishlist(buyerId, bikeId);
            return ResponseEntity.ok(Map.of("message", "Removed from wishlist"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{buyerId}/wishlist")
    @Operation(summary = "Lấy danh sách yêu thích")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    public ResponseEntity<?> getWishlist(
            @Parameter(description = "ID của Buyer") @PathVariable Long buyerId) {
        try {
            List<Wishlist> wishlist = buyerService.getWishlist(buyerId);
            return ResponseEntity.ok(wishlist);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{buyerId}/wishlist/{bikeId}")
    @Operation(summary = "Kiểm tra xe có trong wishlist hay không")
    @ApiResponse(responseCode = "200", description = "Kiểm tra thành công")
    public ResponseEntity<?> isInWishlist(
            @Parameter(description = "ID của Buyer") @PathVariable Long buyerId,
            @Parameter(description = "ID của Bike") @PathVariable Long bikeId) {
        try {
            boolean inWishlist = buyerService.isInWishlist(buyerId, bikeId);
            return ResponseEntity.ok(Map.of("inWishlist", inWishlist));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== SEARCH & FILTER ====================
    @GetMapping("/search-advanced")
    @Operation(summary = "Tìm kiếm & lọc xe nâng cao",
            description = "Tìm kiếm xe theo giá, hãng, loại, năm sản xuất, tình trạng")
    @ApiResponse(responseCode = "200", description = "Tìm kiếm thành công")
    public ResponseEntity<?> searchBikesAdvanced(
            @Parameter(description = "Giá tối thiểu") @RequestParam(required = false) Long minPrice,
            @Parameter(description = "Giá tối đa") @RequestParam(required = false) Long maxPrice,
            @Parameter(description = "Hãng xe (Brand)") @RequestParam(required = false) String brand,
            @Parameter(description = "Loại xe (Road, MTB, Gravel...)") @RequestParam(required = false) String bikeType,
            @Parameter(description = "Năm sản xuất tối thiểu") @RequestParam(required = false) Integer minYear,
            @Parameter(description = "Tình trạng xe") @RequestParam(required = false) String condition,
            @Parameter(description = "Số trang (bắt đầu từ 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số phần tử trên trang") @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Bike> bikes = buyerService.searchBikesAdvanced(
                    minPrice, maxPrice, brand, bikeType, minYear, condition, pageable);
            return ResponseEntity.ok(bikes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/bikes/{bikeId}")
    @Operation(summary = "Xem chi tiết xe",
            description = "Xem thông tin chi tiết của xe (tăng view count)")
    @ApiResponse(responseCode = "200", description = "Lấy chi tiết thành công")
    public ResponseEntity<?> viewBikeDetail(
            @Parameter(description = "ID của Bike") @PathVariable Long bikeId) {
        try {
            Bike bike = buyerService.viewBikeDetail(bikeId);
            return ResponseEntity.ok(bike);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== INSPECTION ====================
    @PostMapping("/{buyerId}/inspection/request/{bikeId}")
    @Operation(summary = "Yêu cầu kiểm định xe")
    @ApiResponse(responseCode = "201", description = "Yêu cầu tạo thành công")
    public ResponseEntity<?> requestInspection(
            @Parameter(description = "ID của Buyer") @PathVariable Long buyerId,
            @Parameter(description = "ID của Bike") @PathVariable Long bikeId,
            @Parameter(description = "Phí kiểm định") @RequestParam Long inspectionFee) {
        try {
            Inspection inspection = buyerService.requestInspection(buyerId, bikeId, inspectionFee);
            return ResponseEntity.status(HttpStatus.CREATED).body(inspection);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/inspection/{bikeId}/approved")
    @Operation(summary = "Lấy báo cáo kiểm định đã phê duyệt")
    @ApiResponse(responseCode = "200", description = "Lấy báo cáo thành công")
    public ResponseEntity<?> getApprovedInspection(
            @Parameter(description = "ID của Bike") @PathVariable Long bikeId) {
        try {
            var inspection = buyerService.getLatestInspectionApproved(bikeId);
            return inspection.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/bikes/{bikeId}/inspections")
    @Operation(summary = "Xem tất cả báo cáo kiểm định của xe")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    public ResponseEntity<?> getBikeInspections(
            @Parameter(description = "ID của Bike") @PathVariable Long bikeId) {
        try {
            List<Inspection> inspections = buyerService.getInspectionsByBike(bikeId);
            return ResponseEntity.ok(inspections);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== PURCHASE & TRANSACTION ====================
    @PostMapping("/{buyerId}/purchase/{bikeId}")
    @Operation(summary = "Đặt mua / thanh toán cọc xe",
            description = "Buyer gửi yêu cầu mua xe với số tiền cọc")
    @ApiResponse(responseCode = "201", description = "Đặt mua thành công")
    public ResponseEntity<?> makePurchase(
            @Parameter(description = "ID của Buyer") @PathVariable Long buyerId,
            @Parameter(description = "ID của Bike") @PathVariable Long bikeId,
            @Parameter(description = "Số tiền cọc") @RequestParam Long depositAmount) {
        try {
            Transaction transaction = buyerService.makePurchase(buyerId, bikeId, depositAmount);
            return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/transaction/{transactionId}")
    @Operation(summary = "Theo dõi giao dịch")
    @ApiResponse(responseCode = "200", description = "Lấy thông tin giao dịch thành công")
    public ResponseEntity<?> trackTransaction(
            @Parameter(description = "ID của Transaction") @PathVariable Long transactionId) {
        try {
            Transaction transaction = buyerService.trackTransaction(transactionId);
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{buyerId}/transactions")
    @Operation(summary = "Xem tất cả giao dịch của Buyer")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách giao dịch thành công")
    public ResponseEntity<?> getBuyerTransactions(
            @Parameter(description = "ID của Buyer") @PathVariable Long buyerId) {
        try {
            List<Transaction> transactions = buyerService.getBuyerTransactions(buyerId);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/transaction/{transactionId}/complete")
    @Operation(summary = "Hoàn tất giao dịch")
    @ApiResponse(responseCode = "200", description = "Hoàn tất thành công")
    public ResponseEntity<?> completeTransaction(
            @Parameter(description = "ID của Transaction") @PathVariable Long transactionId) {
        try {
            Transaction transaction = buyerService.completeTransaction(transactionId);
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/transaction/{transactionId}/cancel")
    @Operation(summary = "Hủy đặt mua / hoàn tiền cọc")
    @ApiResponse(responseCode = "200", description = "Hủy thành công")
    public ResponseEntity<?> cancelPurchase(
            @Parameter(description = "ID của Transaction") @PathVariable Long transactionId) {
        try {
            Transaction transaction = buyerService.cancelPurchase(transactionId);
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== RATING & REVIEW ====================
    @PostMapping("/transaction/{transactionId}/rate")
    @Operation(summary = "Đánh giá Seller sau giao dịch",
            description = "Buyer đánh giá Seller với sao từ 1-5 và review text")
    @ApiResponse(responseCode = "200", description = "Đánh giá thành công")
    public ResponseEntity<?> rateTransaction(
            @Parameter(description = "ID của Transaction") @PathVariable Long transactionId,
            @Parameter(description = "Điểm đánh giá (1-5)") @RequestParam Double rating,
            @Parameter(description = "Nội dung đánh giá") @RequestParam String review) {
        try {
            Transaction transaction = buyerService.rateTransaction(transactionId, rating, review);
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== REPORT & DISPUTE ====================
    @PostMapping("/{buyerId}/report")
    @Operation(summary = "Báo cáo vi phạm",
            description = "Buyer báo cáo xe hoặc user vi phạm chính sách")
    @ApiResponse(responseCode = "201", description = "Báo cáo thành công")
    public ResponseEntity<?> submitReport(
            @Parameter(description = "ID của Buyer") @PathVariable Long buyerId,
            @Parameter(description = "ID của Bike (nếu báo cáo xe)") @RequestParam(required = false) Long bikeId,
            @Parameter(description = "ID của User bị báo cáo") @RequestParam(required = false) Long reportedUserId,
            @Parameter(description = "Loại báo cáo") @RequestParam Report.ReportType reportType,
            @Parameter(description = "Nội dung báo cáo") @RequestParam String description) {
        try {
            Report report = buyerService.submitReport(buyerId, bikeId, reportedUserId, reportType, description);
            return ResponseEntity.status(HttpStatus.CREATED).body(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{buyerId}/reports")
    @Operation(summary = "Xem lịch sử báo cáo của tôi")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách báo cáo thành công")
    public ResponseEntity<?> getMyReports(
            @Parameter(description = "ID của Buyer") @PathVariable Long buyerId) {
        try {
            List<Report> reports = buyerService.getMyReports(buyerId);
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/transaction/{transactionId}/dispute")
    @Operation(summary = "Tranh chấp giao dịch",
            description = "Buyer yêu cầu tranh chấp giao dịch (hoàn tiền)")
    @ApiResponse(responseCode = "200", description = "Tranh chấp được ghi nhận")
    public ResponseEntity<?> disputeTransaction(
            @Parameter(description = "ID của Transaction") @PathVariable Long transactionId,
            @Parameter(description = "Lý do tranh chấp") @RequestParam String reason) {
        try {
            Transaction transaction = buyerService.disputeTransaction(transactionId, reason);
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
