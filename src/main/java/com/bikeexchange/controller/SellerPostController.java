package com.bikeexchange.controller;

import com.bikeexchange.dto.request.SellerPostCreateRequest;
import com.bikeexchange.dto.request.SellerPostUpdateRequest;
import com.bikeexchange.dto.response.PostResponse;
import com.bikeexchange.model.Post;
import com.bikeexchange.security.UserPrincipal;
import com.bikeexchange.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/seller/posts")
@Tag(name = "Seller Post Management", description = "APIs for sellers to manage their posts")
@SecurityRequirement(name = "Bearer Token")
public class SellerPostController {

    @Autowired
    private PostService postService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List posts", description = "List posts for the authenticated seller.")
    public ResponseEntity<?> list(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Filter by statuses (repeat or comma-separated). Example: status=ACTIVE&status=VERIFIED", example = "ACTIVE") @RequestParam(required = false) java.util.List<String> status,
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20") @RequestParam(defaultValue = "20") int size) {
        Long sellerId = currentUser.getId();
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> result = postService.listPosts(sellerId, status, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", result.map(PostResponse::fromEntity));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get seller post detail", description = "Fetch details of a post owned by the authenticated seller.")
    public ResponseEntity<?> getOne(
            @Parameter(description = "Post id", example = "10") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
        Long sellerId = currentUser.getId();
        Post post = postService.getSellerPost(id, sellerId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", PostResponse.fromEntity(post));
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Create seller post", description = "Create a post from an existing bike owned by the seller. Must have SELLER role.")
    public ResponseEntity<?> create(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody SellerPostCreateRequest request) {
        Long sellerId = currentUser.getId();
        Post post = postService.createPost(sellerId, request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Post created successfully");
        response.put("data", PostResponse.fromEntity(post));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Update seller post", description = "Update a post owned by the authenticated seller.")
    public ResponseEntity<?> update(
            @Parameter(description = "Post id", example = "10") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody SellerPostUpdateRequest request) {
        Long sellerId = currentUser.getId();
        Post post = postService.updatePost(id, sellerId, request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Post updated successfully");
        response.put("data", PostResponse.fromEntity(post));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Delete seller post", description = "Cancel a post owned by the authenticated seller.")
    public ResponseEntity<?> delete(
            @Parameter(description = "Post id", example = "10") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
        Long sellerId = currentUser.getId();
        postService.deletePost(id, sellerId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Post cancelled successfully");
        return ResponseEntity.ok(response);
    }
}
