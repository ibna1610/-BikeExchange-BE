package com.bikeexchange.dto.response;

import com.bikeexchange.model.InspectionRequest;
import lombok.Data;
import java.util.List;

@Data
public class SystemWalletSummaryResponse {
    private Long totalSystemAvailablePoints;
    private Long totalSystemFrozenPoints;
    
    // 1. Escrow (Orders holding money)
    private Integer escrowOrdersCount;
    private Long totalEscrowPoints;
    private List<OrderResponse> escrowOrders;

    // 2. Withdrawals (Users requesting cash out)
    private Integer pendingWithdrawalsCount;
    private Long totalPendingWithdrawalPoints;
    private List<PointTransactionDto> pendingWithdrawals;

    // 3. Inspection (Sellers holding money for inspection)
    private Integer activeInspectionsCount;
    private Long totalInspectionFeePoints;
    private List<InspectionRequest> activeInspections;

    // Verification Logic
    private Boolean isBalanced;
    private String balanceCheckMessage;
}
