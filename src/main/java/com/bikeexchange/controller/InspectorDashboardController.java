package com.bikeexchange.controller;

import com.bikeexchange.dto.response.InspectorDashboardResponse;
import com.bikeexchange.security.UserPrincipal;
import com.bikeexchange.service.service.InspectorDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/inspector")
@PreAuthorize("hasRole('INSPECTOR')")
@Tag(name = "Inspector - Dashboard", description = "Dashboard and statistics for inspectors")
@SecurityRequirement(name = "Bearer Token")
public class InspectorDashboardController {

    @Autowired
    private InspectorDashboardService inspectorDashboardService;

    @GetMapping("/dashboard")
    @Operation(summary = "Lấy dữ liệu dashboard cho inspector", 
               description = "Trả về thống kê số lượng kiểm định, các task đang thực hiện và các task gần đây.")
    public ResponseEntity<?> getDashboard(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
        
        InspectorDashboardResponse data = inspectorDashboardService.getDashboardData(currentUser.getId());
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", data
        ));
    }
}
