package com.bikeexchange.dto.response;

import com.bikeexchange.model.History;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HistoryResponse {
    private Long id;
    private String entityType;
    private Long entityId;
    private String action;
    private Long performedBy;
    private String metadata;
    private LocalDateTime timestamp;

    public static HistoryResponse fromEntity(History history) {
        HistoryResponse res = new HistoryResponse();
        res.setId(history.getId());
        res.setEntityType(history.getEntityType());
        res.setEntityId(history.getEntityId());
        res.setAction(history.getAction());
        res.setPerformedBy(history.getPerformedBy());
        res.setMetadata(history.getMetadata());
        res.setTimestamp(history.getTimestamp());
        return res;
    }
}
