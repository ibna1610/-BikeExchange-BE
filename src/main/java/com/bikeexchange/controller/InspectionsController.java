package com.bikeexchange.controller;

import com.bikeexchange.dto.request.InspectionRequestDto;
import com.bikeexchange.model.InspectionRequest;
import com.bikeexchange.security.UserPrincipal;
import com.bikeexchange.repository.InspectionRepository;
import com.bikeexchange.service.InspectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/inspections")
public class InspectionsController {

    @Autowired
    private InspectionRepository inspectionRepository;

    @Autowired
    private InspectionService inspectionService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> list(@RequestParam(required = false) Long bike_id,
                                  @RequestParam(required = false) Long inspector_id,
                                  @RequestParam(required = false) InspectionRequest.RequestStatus status,
                                  @RequestParam(required = false) String date_from,
                                  @RequestParam(required = false) String date_to,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Specification<InspectionRequest> spec = Specification.where(null);

        if (bike_id != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("listing").get("id"), bike_id));
        }
        if (inspector_id != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("inspector").get("id"), inspector_id));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (date_from != null && !date_from.isBlank()) {
            LocalDateTime from = LocalDateTime.parse(date_from);
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (date_to != null && !date_to.isBlank()) {
            LocalDateTime to = LocalDateTime.parse(date_to);
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }

        Page<InspectionRequest> result = inspectionRepository.findAll(spec, pageable);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", result);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> create(@AuthenticationPrincipal UserPrincipal currentUser,
                                    @RequestBody InspectionRequestDto request) {
        InspectionRequest inspection = inspectionService.requestInspection(currentUser.getId(), request.getListingId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", inspection);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getOne(@PathVariable Long id) {
        return inspectionRepository.findById(id)
                .map(i -> ResponseEntity.ok(Map.of("success", true, "data", i)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('INSPECTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @RequestParam InspectionRequest.RequestStatus status,
                                          @AuthenticationPrincipal UserPrincipal currentUser) {
        InspectionRequest inspection = inspectionRepository.findById(id)
                .orElse(null);
        if (inspection == null) {
            return ResponseEntity.notFound().build();
        }
        if (status == InspectionRequest.RequestStatus.IN_PROGRESS) {
            inspection.setStatus(InspectionRequest.RequestStatus.IN_PROGRESS);
            inspection.setStartedAt(LocalDateTime.now());
        } else if (status == InspectionRequest.RequestStatus.INSPECTED) {
            inspection.setStatus(InspectionRequest.RequestStatus.INSPECTED);
            inspection.setCompletedAt(LocalDateTime.now());
        } else if (status == InspectionRequest.RequestStatus.ASSIGNED && currentUser != null) {
            inspection = inspectionService.assignInspector(id, currentUser.getId());
        } else {
            inspection.setStatus(status);
        }
        inspectionRepository.save(inspection);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", inspection);
        return ResponseEntity.ok(response);
    }
}
