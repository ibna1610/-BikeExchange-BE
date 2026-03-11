package com.bikeexchange.controller;

import com.bikeexchange.model.Bike;
import com.bikeexchange.model.Post;
import com.bikeexchange.repository.BikeRepository;
import com.bikeexchange.dto.response.PostResponse;
import com.bikeexchange.security.UserPrincipal;
import com.bikeexchange.service.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Bikes", description = "6.2 Kiểm duyệt tin đăng")
@SecurityRequirement(name = "Bearer Token")
public class AdminBikeController extends AdminBaseController {

    @Autowired private BikeRepository bikeRepository;
    @Autowired private PostService postService;

    @GetMapping("/bikes/pending")
    @Operation(summary = "Danh sách tin chờ duyệt")
    public ResponseEntity<?> listPendingBikes(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        int pageNo = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        return ok("Pending bikes retrieved successfully",
                bikeRepository.findByStatus(Bike.BikeStatus.DRAFT, pageable));
    }

    @GetMapping("/bikes/{id}")
    @Operation(summary = "Xem chi tiết tin")
    public ResponseEntity<?> getBikeById(@PathVariable Long id) {
        return bikeRepository.findById(id)
                .<ResponseEntity<?>>map(bike -> ok("Bike retrieved successfully", bike))
                .orElseGet(() -> notFound("Bike not found"));
    }

    @PutMapping("/bikes/{id}/approve")
    @Operation(summary = "Duyệt tin")
    public ResponseEntity<?> approveBike(@PathVariable Long id) {
        return bikeRepository.findById(id).<ResponseEntity<?>>map(bike -> {
            bike.setStatus(Bike.BikeStatus.ACTIVE);
            return ok("Bike approved", bikeRepository.save(bike));
        }).orElseGet(() -> notFound("Bike not found"));
    }

    @PutMapping("/bikes/{id}/reject")
    @Operation(summary = "Từ chối tin")
    public ResponseEntity<?> rejectBike(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        String reasonValue = (reason == null || reason.isBlank()) ? "Rejected by admin" : reason;
        return bikeRepository.findById(id).<ResponseEntity<?>>map(bike -> {
            bike.setStatus(Bike.BikeStatus.CANCELLED);
            bikeRepository.save(bike);
            return ok("Bike rejected", Map.of("bikeId", id, "reason", reasonValue));
        }).orElseGet(() -> notFound("Bike not found"));
    }

    @PutMapping("/bikes/{id}/hide")
    @Operation(summary = "Ẩn tin vi phạm")
    public ResponseEntity<?> hideBike(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        String reasonValue = (reason == null || reason.isBlank()) ? "Hidden by admin" : reason;
        return rejectBike(id, reasonValue);
    }

    @GetMapping("/listings")
    @Operation(summary = "Danh sách tất cả tin đăng")
    public ResponseEntity<?> listListings(
            @Parameter(
                description = "Filter theo trang thai tin dang",
                    schema = @Schema(implementation = Post.PostStatus.class))
            @RequestParam(required = false) Post.PostStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        int pageNo = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        List<String> statusFilter = status != null ? Collections.singletonList(status.name()) : null;
        var result = postService.listPosts(null, statusFilter, pageable);
        return ok("Listings retrieved successfully", result.map(PostResponse::fromEntity));
    }

    @PutMapping("/listings/{postId}")
    @Operation(summary = "Cập nhật trạng thái tin đăng")
    public ResponseEntity<?> updateListing(
            @PathVariable Long postId,
            @RequestParam String action,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Post post;
        if ("APPROVE".equalsIgnoreCase(action)) {
            post = postService.adminApprovePost(postId, currentUser.getId());
        } else if ("REJECT".equalsIgnoreCase(action)) {
            post = postService.adminRejectPost(postId, currentUser.getId(), reason);
        } else {
            post = postService.adminUpdatePostStatus(postId, currentUser.getId(), action, reason);
        }
        return ok("Listing updated", post);
    }

    @DeleteMapping("/listings/{postId}")
    @Operation(summary = "Xóa tin đăng")
    public ResponseEntity<?> deleteListing(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Post post = postService.adminRejectPost(postId, currentUser.getId(), "deleted by admin");
        return ok("Post deleted", post);
    }
}
