package com.bikeexchange.controller;

import com.bikeexchange.dto.request.DisputeCreateRequest;
import com.bikeexchange.dto.request.DisputeResolveRequest;
import com.bikeexchange.model.Dispute;
import com.bikeexchange.security.UserPrincipal;
import com.bikeexchange.service.DisputeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping
@Tag(name = "Dispute Management", description = "APIs for creating and resolving disputes")
@SecurityRequirement(name = "Bearer Token")
public class DisputeController {

    @Autowired
    private DisputeService disputeService;

    @PostMapping("/dispute")
    public ResponseEntity<?> createDispute(@AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody DisputeCreateRequest request) {
        Dispute dispute = disputeService.createDispute(currentUser.getId(), request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Dispute opened successfully");
        response.put("data", dispute);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/dispute/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resolveDispute(@PathVariable Long id,
            @RequestBody DisputeResolveRequest request) {
        Dispute dispute = disputeService.resolveDispute(id, request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Dispute resolved successfully as " + request.getResolutionType());
        response.put("data", dispute);

        return ResponseEntity.ok(response);
    }
}
