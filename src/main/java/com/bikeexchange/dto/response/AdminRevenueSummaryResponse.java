package com.bikeexchange.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class AdminRevenueSummaryResponse {
    private Long totalRevenue;
    
    // 1. Phí đăng tin lẻ (Single Posting Fees)
    private Long postingFeesTotal;
    private Integer postingFeesCount;
    private List<PointTransactionDto> postingFeeDetails;

    // 2. Phí mua gói combo (Listing Combo Purchases)
    private Long comboFeesTotal;
    private Integer comboFeesCount;
    private List<PointTransactionDto> comboFeeDetails;

    // 3. Phí nâng cấp Seller (Seller Upgrade Fees)
    private Long upgradeFeesTotal;
    private Integer upgradeFeesCount;
    private List<PointTransactionDto> upgradeFeeDetails;

    // 4. Phí kiểm định hệ thống thực thu (Inspection Fees)
    private Long inspectionFeesTotal;
    private Integer inspectionFeesCount;
    private List<InspectionResponse> inspectionFeeDetails;

    // 5. Hoa hồng từ đơn hàng (Order Commission)
    private Long orderCommissionTotal;
    private Integer orderCommissionCount;
    private List<PointTransactionDto> commissionDetails;
}
