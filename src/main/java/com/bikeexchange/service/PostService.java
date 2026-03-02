package com.bikeexchange.service;

import com.bikeexchange.dto.request.SellerPostCreateRequest;
import com.bikeexchange.dto.request.SellerPostUpdateRequest;
import com.bikeexchange.exception.InsufficientBalanceException;
import com.bikeexchange.exception.ResourceNotFoundException;
import com.bikeexchange.model.Bike;
import com.bikeexchange.model.PointTransaction;
import com.bikeexchange.model.Post;
import com.bikeexchange.model.User;
import com.bikeexchange.model.UserWallet;
import com.bikeexchange.repository.BikeRepository;
import com.bikeexchange.repository.PointTransactionRepository;
import com.bikeexchange.repository.PostRepository;
import com.bikeexchange.repository.UserRepository;
import com.bikeexchange.repository.UserWalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private BikeRepository bikeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserWalletRepository walletRepository;

    @Autowired
    private PointTransactionRepository pointTxRepo;

    @Autowired
    private InspectionService inspectionService;

    @Autowired
    private HistoryService historyService;

    private static final Long POST_FEE_STANDARD = 10L;
    private static final Long POST_FEE_VERIFIED = 30L;

    public Page<Post> listSellerPosts(Long sellerId, List<String> statusParams, Pageable pageable) {
        List<Post.PostStatus> statuses;
        if (statusParams != null && !statusParams.isEmpty()) {
            statuses = statusParams.stream()
                    .map(s -> {
                        try {
                            return Post.PostStatus.valueOf(s.trim().toUpperCase());
                        } catch (IllegalArgumentException ex) {
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();
            if (statuses.isEmpty()) {
                statuses = java.util.Arrays.asList(Post.PostStatus.values());
            }
        } else {
            statuses = java.util.Arrays.asList(Post.PostStatus.values());
        }
        return postRepository.findBySellerIdAndStatusIn(sellerId, statuses, pageable);
    }

    public Page<Post> listPosts(Long sellerId, List<String> statusParams, Pageable pageable) {
        List<Post.PostStatus> statuses;
        if (statusParams != null && !statusParams.isEmpty()) {
            statuses = statusParams.stream()
                    .map(s -> {
                        try {
                            return Post.PostStatus.valueOf(s.trim().toUpperCase());
                        } catch (IllegalArgumentException ex) {
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();
            if (statuses.isEmpty()) {
                statuses = java.util.Arrays.asList(Post.PostStatus.values());
            }
        } else {
            statuses = java.util.Arrays.asList(Post.PostStatus.values());
        }

        if (sellerId == null) {
            return postRepository.findByStatusIn(statuses, pageable);
        }
        return postRepository.findBySellerIdAndStatusIn(sellerId, statuses, pageable);
    }

    public Post getSellerPost(Long postId, Long sellerId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
        if (!post.getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("Only the seller can access this post");
        }
        return post;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Post createPost(Long sellerId, SellerPostCreateRequest request) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));

        Bike bike = bikeRepository.findByIdForUpdate(request.getBikeId())
                .orElseThrow(() -> new ResourceNotFoundException("Bike not found with id: " + request.getBikeId()));

        if (!bike.getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("Only the seller can post this bike");
        }

        if (postRepository.existsByBikeIdAndStatusIn(bike.getId(), java.util.Arrays.asList(Post.PostStatus.ACTIVE))) {
            throw new IllegalStateException("Bike already has an active post");
        }

        if (bike.getStatus() == Bike.BikeStatus.RESERVED || bike.getStatus() == Bike.BikeStatus.SOLD) {
            throw new IllegalStateException("Bike is not eligible for posting");
        }

        Post.ListingType listingType = parseListingType(request.getListingType());
        Long postFee = listingType == Post.ListingType.VERIFIED ? POST_FEE_VERIFIED : POST_FEE_STANDARD;

        UserWallet wallet = walletRepository.findByUserIdForUpdate(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet missing"));

        if (wallet.getAvailablePoints() < postFee) {
            throw new InsufficientBalanceException("Not enough points to create post. Required: " + postFee);
        }

        wallet.setAvailablePoints(wallet.getAvailablePoints() - postFee);
        walletRepository.save(wallet);

        if (listingType == Post.ListingType.VERIFIED) {
            bike.setStatus(Bike.BikeStatus.DRAFT);
            bike.setInspectionStatus(Bike.InspectionStatus.REQUESTED);
        } else {
            bike.setStatus(Bike.BikeStatus.ACTIVE);
            bike.setInspectionStatus(Bike.InspectionStatus.NONE);
        }
        bike.setUpdatedAt(LocalDateTime.now());
        bikeRepository.save(bike);

        Post post = new Post();
        post.setSeller(seller);
        post.setBike(bike);
        post.setCaption(request.getCaption());
        post.setListingType(listingType);
        post.setStatus(Post.PostStatus.ACTIVE);

        Post saved = postRepository.save(post);

        PointTransaction tx = new PointTransaction();
        tx.setUser(wallet.getUser());
        tx.setAmount(postFee);
        tx.setType(PointTransaction.TransactionType.SPEND);
        tx.setStatus(PointTransaction.TransactionStatus.SUCCESS);
        tx.setReferenceId("Post fee: " + saved.getId());
        pointTxRepo.save(tx);

        historyService.log("post", saved.getId(), "created", seller.getId(), null);

        if (listingType == Post.ListingType.VERIFIED) {
            com.bikeexchange.dto.request.InspectionRequestDto inspectionDto = new com.bikeexchange.dto.request.InspectionRequestDto();
            inspectionDto.setBikeId(bike.getId());
            inspectionService.requestInspection(sellerId, inspectionDto);
        }
        return saved;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Post updatePost(Long postId, Long sellerId, SellerPostUpdateRequest request) {
        Post post = getSellerPost(postId, sellerId);
        post.setCaption(request.getCaption());
        return postRepository.save(post);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Post adminApprovePost(Long postId, Long adminId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        post.setStatus(Post.PostStatus.ACTIVE);
        Post saved = postRepository.save(post);
        historyService.log("post", saved.getId(), "approved", adminId, null);
        return saved;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Post adminRejectPost(Long postId, Long adminId, String reason) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        post.setStatus(Post.PostStatus.CANCELLED);
        Post saved = postRepository.save(post);
        historyService.log("post", saved.getId(), "rejected", adminId, reason);
        return saved;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deletePost(Long postId, Long sellerId) {
        Post post = getSellerPost(postId, sellerId);
        post.setStatus(Post.PostStatus.CANCELLED);
        postRepository.save(post);

        // Also revert the bike status to DRAFT so it doesn't appear as ACTIVE/VERIFIED
        // in search without an active post
        Bike bike = post.getBike();
        if (bike.getStatus() != Bike.BikeStatus.SOLD && bike.getStatus() != Bike.BikeStatus.RESERVED) {
            bike.setStatus(Bike.BikeStatus.DRAFT);
            bikeRepository.save(bike);
        }

        historyService.log("post", post.getId(), "cancelled", sellerId, null);
    }

    private Post.ListingType parseListingType(String listingType) {
        if (listingType == null) {
            return Post.ListingType.STANDARD;
        }
        try {
            return Post.ListingType.valueOf(listingType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Post.ListingType.STANDARD;
        }
    }
}
