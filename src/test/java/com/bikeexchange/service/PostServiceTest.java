package com.bikeexchange.service;

import com.bikeexchange.dto.request.SellerPostCreateRequest;
import com.bikeexchange.dto.request.SellerPostUpdateRequest;
import com.bikeexchange.exception.InsufficientBalanceException;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bikeexchange.dto.request.InspectionRequestDto;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private BikeRepository bikeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserWalletRepository walletRepository;

    @Mock
    private PointTransactionRepository pointTxRepo;

    @Mock
    private InspectionService inspectionService;

    @Mock
    private HistoryService historyService;

    @InjectMocks
    private PostService postService;

    @Test
    public void createPost_standard_deductsFee_setsActive() {
        User seller = new User();
        seller.setId(1L);
        UserWallet wallet = new UserWallet();
        wallet.setUser(seller);
        wallet.setAvailablePoints(100L);

        Bike bike = new Bike();
        bike.setId(10L);
        bike.setSeller(seller);
        bike.setStatus(Bike.BikeStatus.DRAFT);
        bike.setInspectionStatus(Bike.InspectionStatus.NONE);

        SellerPostCreateRequest request = new SellerPostCreateRequest();
        request.setBikeId(10L);
        request.setCaption("Desc");
        request.setListingType("STANDARD");

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        Mockito.when(bikeRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(bike));
        Mockito.when(postRepository.existsByBikeIdAndStatusIn(Mockito.eq(10L), Mockito.anyList())).thenReturn(false);
        Mockito.when(walletRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(wallet));
        Mockito.when(bikeRepository.save(Mockito.any(Bike.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(postRepository.save(Mockito.any(Post.class))).thenAnswer(invocation -> {
            Post p = invocation.getArgument(0);
            p.setId(100L);
            return p;
        });
        Mockito.when(pointTxRepo.save(Mockito.any(PointTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Post saved = postService.createPost(1L, request);

        Assertions.assertEquals(Post.PostStatus.ACTIVE, saved.getStatus());
        Assertions.assertEquals(Post.ListingType.STANDARD, saved.getListingType());
        Assertions.assertEquals(100L, saved.getId());
        Assertions.assertEquals(Bike.BikeStatus.ACTIVE, bike.getStatus());

        ArgumentCaptor<UserWallet> walletCaptor = ArgumentCaptor.forClass(UserWallet.class);
        Mockito.verify(walletRepository).save(walletCaptor.capture());
        Assertions.assertEquals(90L, walletCaptor.getValue().getAvailablePoints());

        ArgumentCaptor<PointTransaction> txCaptor = ArgumentCaptor.forClass(PointTransaction.class);
        Mockito.verify(pointTxRepo).save(txCaptor.capture());
        Assertions.assertEquals(10L, txCaptor.getValue().getAmount());
        Assertions.assertEquals(PointTransaction.TransactionType.SPEND, txCaptor.getValue().getType());

        Mockito.verify(inspectionService, Mockito.never()).requestInspection(Mockito.anyLong(),
                Mockito.any(InspectionRequestDto.class));
    }

    @Test
    public void createPost_verified_requestsInspection() {
        User seller = new User();
        seller.setId(1L);
        UserWallet wallet = new UserWallet();
        wallet.setUser(seller);
        wallet.setAvailablePoints(100L);

        Bike bike = new Bike();
        bike.setId(10L);
        bike.setSeller(seller);
        bike.setStatus(Bike.BikeStatus.DRAFT);
        bike.setInspectionStatus(Bike.InspectionStatus.NONE);

        SellerPostCreateRequest request = new SellerPostCreateRequest();
        request.setBikeId(10L);
        request.setCaption("Desc");
        request.setListingType("VERIFIED");

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        Mockito.when(bikeRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(bike));
        Mockito.when(postRepository.existsByBikeIdAndStatusIn(Mockito.eq(10L), Mockito.anyList())).thenReturn(false);
        Mockito.when(walletRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(wallet));
        Mockito.when(bikeRepository.save(Mockito.any(Bike.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(postRepository.save(Mockito.any(Post.class))).thenAnswer(invocation -> {
            Post p = invocation.getArgument(0);
            p.setId(100L);
            return p;
        });
        Mockito.when(pointTxRepo.save(Mockito.any(PointTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Post saved = postService.createPost(1L, request);

        Assertions.assertEquals(Post.ListingType.VERIFIED, saved.getListingType());
        Assertions.assertEquals(Bike.InspectionStatus.REQUESTED, bike.getInspectionStatus());

        ArgumentCaptor<UserWallet> walletCaptor = ArgumentCaptor.forClass(UserWallet.class);
        Mockito.verify(walletRepository).save(walletCaptor.capture());
        Assertions.assertEquals(70L, walletCaptor.getValue().getAvailablePoints());

        ArgumentCaptor<PointTransaction> txCaptor = ArgumentCaptor.forClass(PointTransaction.class);
        Mockito.verify(pointTxRepo).save(txCaptor.capture());
        Assertions.assertEquals(30L, txCaptor.getValue().getAmount());
        Assertions.assertEquals(PointTransaction.TransactionType.SPEND, txCaptor.getValue().getType());

        ArgumentCaptor<InspectionRequestDto> inspectionDtoCaptor = ArgumentCaptor.forClass(InspectionRequestDto.class);
        Mockito.verify(inspectionService).requestInspection(Mockito.eq(1L), inspectionDtoCaptor.capture());
        Assertions.assertEquals(10L, inspectionDtoCaptor.getValue().getBikeId());
    }

    @Test
    public void createPost_insufficientPoints_throws() {
        User seller = new User();
        seller.setId(1L);
        UserWallet wallet = new UserWallet();
        wallet.setUser(seller);
        wallet.setAvailablePoints(5L);

        Bike bike = new Bike();
        bike.setId(10L);
        bike.setSeller(seller);
        bike.setStatus(Bike.BikeStatus.DRAFT);
        bike.setInspectionStatus(Bike.InspectionStatus.NONE);

        SellerPostCreateRequest request = new SellerPostCreateRequest();
        request.setBikeId(10L);
        request.setCaption("Desc");
        request.setListingType("STANDARD");

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        Mockito.when(bikeRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(bike));
        Mockito.when(postRepository.existsByBikeIdAndStatusIn(Mockito.eq(10L), Mockito.anyList())).thenReturn(false);
        Mockito.when(walletRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(wallet));

        Assertions.assertThrows(InsufficientBalanceException.class, () -> postService.createPost(1L, request));
        Mockito.verify(postRepository, Mockito.never()).save(Mockito.any(Post.class));
    }

    @Test
    public void updatePost_updatesCaption() {
        User seller = new User();
        seller.setId(1L);
        Post post = new Post();
        post.setId(100L);
        post.setSeller(seller);
        post.setCaption("Old");
        post.setListingType(Post.ListingType.STANDARD);
        post.setStatus(Post.PostStatus.ACTIVE);

        SellerPostUpdateRequest request = new SellerPostUpdateRequest();
        request.setCaption("New");

        Mockito.when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        Mockito.when(postRepository.save(Mockito.any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Post saved = postService.updatePost(100L, 1L, request);
        Assertions.assertEquals("New", saved.getCaption());
    }

    @Test
    public void listPosts_withoutSellerId_usesStatusFilter() {
        Post post = new Post();
        post.setId(100L);
        post.setStatus(Post.PostStatus.ACTIVE);

        Mockito.when(postRepository.findByStatusIn(Mockito.anyList(),
                Mockito.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(post)));

        org.springframework.data.domain.Page<Post> result = postService.listPosts(null, java.util.List.of("ACTIVE"),
                org.springframework.data.domain.PageRequest.of(0, 20));

        Assertions.assertEquals(1, result.getTotalElements());
        Assertions.assertEquals(100L, result.getContent().get(0).getId());
    }

    @Test
    public void adminApprovePost_setsActive() {
        Post post = new Post();
        post.setId(200L);
        post.setStatus(Post.PostStatus.CANCELLED);
        Mockito.when(postRepository.findById(200L)).thenReturn(Optional.of(post));
        Mockito.when(postRepository.save(Mockito.any(Post.class))).thenAnswer(i -> i.getArgument(0));

        Post saved = postService.adminApprovePost(200L, 1L);
        Assertions.assertEquals(Post.PostStatus.ACTIVE, saved.getStatus());
    }

    @Test
    public void adminRejectPost_setsCancelled() {
        Post post = new Post();
        post.setId(300L);
        post.setStatus(Post.PostStatus.ACTIVE);
        Mockito.when(postRepository.findById(300L)).thenReturn(Optional.of(post));
        Mockito.when(postRepository.save(Mockito.any(Post.class))).thenAnswer(i -> i.getArgument(0));

        Post saved = postService.adminRejectPost(300L, 1L, "bad");
        Assertions.assertEquals(Post.PostStatus.CANCELLED, saved.getStatus());
    }
}
