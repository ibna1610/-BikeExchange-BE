package com.bikeexchange.dto.response;

import com.bikeexchange.model.PointTransaction;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PointTransactionDto {
    private Long id;
    private Long userId;
    private Long amount;
    private String type;
    private String status;
    private String referenceId;
    private String remarks;
    private String userEmail;
    private String userFullName;
    private LocalDateTime createdAt;

    public static PointTransactionDto from(PointTransaction tx) {
        PointTransactionDto d = new PointTransactionDto();
        d.id = tx.getId();
        d.userId = tx.getUser() != null ? tx.getUser().getId() : null;
        d.amount = tx.getAmount();
        d.type = tx.getType() != null ? tx.getType().name() : null;
        d.status = tx.getStatus() != null ? tx.getStatus().name() : null;
        d.referenceId = tx.getReferenceId();
        d.remarks = tx.getRemarks();
        if (tx.getUser() != null) {
            d.userEmail = tx.getUser().getEmail();
            d.userFullName = tx.getUser().getFullName();
        }
        d.createdAt = tx.getCreatedAt();
        return d;
    }
}
