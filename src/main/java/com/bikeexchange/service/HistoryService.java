package com.bikeexchange.service;

import com.bikeexchange.model.History;
import com.bikeexchange.repository.HistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HistoryService {

    @Autowired
    private HistoryRepository historyRepository;

    public void log(String entityType, Long entityId, String action, Long performedBy, String metadata) {
        History h = new History();
        h.setEntityType(entityType);
        h.setEntityId(entityId);
        h.setAction(action);
        h.setPerformedBy(performedBy);
        h.setMetadata(metadata);
        historyRepository.save(h);
    }
}
