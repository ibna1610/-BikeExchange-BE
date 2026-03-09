package com.bikeexchange.service.service;

import com.bikeexchange.dto.request.BikeCreateRequest;
import com.bikeexchange.dto.request.BikeMediaRequest;
import com.bikeexchange.exception.InsufficientBalanceException;
import com.bikeexchange.exception.ResourceNotFoundException;
import com.bikeexchange.model.*;
import com.bikeexchange.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BikeService {

    @Autowired
    private BikeRepository bikeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private UserWalletRepository walletRepository;

    @Autowired
    private PointTransactionRepository pointTxRepo;

    public Page<Bike> searchBikes(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return bikeRepository.searchAllStatuses(keyword, pageable);
        }
        return bikeRepository.findAll(pageable);
    }

    public Page<Bike> searchBikesByCategory(Long categoryId, Pageable pageable) {
        return bikeRepository.findByCategories_Id(categoryId, pageable);
    }

    public Page<Bike> searchBikesAdvanced(String keyword, Long categoryId, List<String> statusParams,
            Long minPrice, Long maxPrice, Integer minYear, String frameSize, Pageable pageable) {
        List<Bike.BikeStatus> statuses;
        if (statusParams != null && !statusParams.isEmpty()) {
            statuses = statusParams.stream()
                    .map(s -> {
                        try {
                            return Bike.BikeStatus.valueOf(s.trim().toUpperCase());
                        } catch (IllegalArgumentException ex) {
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();
            if (statuses.isEmpty()) {
                statuses = java.util.Arrays.asList(Bike.BikeStatus.values());
            }
        } else {
            statuses = java.util.Arrays.asList(Bike.BikeStatus.values());
        }

        if (keyword != null && !keyword.isBlank()) {
            if (categoryId != null) {
                return bikeRepository.searchByCategoryKeywordAndStatuses(categoryId, keyword.toLowerCase(), statuses,
                        pageable);
            }
            return bikeRepository.searchByKeywordAndStatuses(keyword.toLowerCase(), statuses, pageable);
        }

        // If price/frame filters provided, use advanced filter
        if (minPrice != null || maxPrice != null || minYear != null || (frameSize != null && !frameSize.isBlank())) {
            return bikeRepository.filterBikesAdvanced(minPrice, maxPrice, minYear, frameSize, pageable);
        }

        if (categoryId != null) {
            return bikeRepository.findByCategories_IdAndStatusIn(categoryId, statuses, pageable);
        }
        return bikeRepository.findByStatusIn(statuses, pageable);
    }

    public Bike getBikeById(Long id) {
        return bikeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bike not found with id: " + id));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Bike createBike(Long sellerId, BikeCreateRequest request) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));

        Bike bike = new Bike();
        bike.setSeller(seller);
        bike.setTitle(request.getTitle());
        bike.setDescription(request.getDescription());

        Brand brandEntity = brandRepository.findByName(request.getBrand())
                .orElseGet(() -> {
                    Brand nb = new Brand();
                    nb.setName(request.getBrand());
                    return brandRepository.save(nb);
                });
        bike.setBrand(brandEntity);

        bike.setModel(request.getModel());
        bike.setYear(request.getYear());
        bike.setPricePoints(request.getPricePoints());
        bike.setCondition(request.getCondition());
        bike.setBikeType(request.getBikeType());
        bike.setFrameSize(request.getFrameSize());
        bike.setStatus(Bike.BikeStatus.ACTIVE);
        bike.setInspectionStatus(Bike.InspectionStatus.NONE);
        bike.setCreatedAt(LocalDateTime.now());
        bike.setMileage(0);
        bike.setLocation("Not specified");

        // Handle Media
        if (request.getMedia() != null && !request.getMedia().isEmpty()) {
            for (int i = 0; i < request.getMedia().size(); i++) {
                BikeMediaRequest mr = request.getMedia().get(i);
                BikeMedia bm = new BikeMedia();
                bm.setBike(bike);
                bm.setUrl(mr.getUrl());
                bm.setType(BikeMedia.MediaType.valueOf(mr.getType().toUpperCase()));
                bm.setSortOrder(mr.getSortOrder() != null ? mr.getSortOrder() : i);
                bike.getMedia().add(bm);
            }
        }

        // Handle Categories
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            Set<Category> categories = new HashSet<>();
            for (Long cid : request.getCategoryIds()) {
                Category cat = categoryRepository.findById(cid)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + cid));
                categories.add(cat);
            }
            bike.setCategories(categories);
        }

        // Deduct 5 points for posting a new bike
        long postFee = 5L;
        UserWallet wallet = walletRepository.findByUserIdForUpdate(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for seller"));

        if (wallet.getAvailablePoints() < postFee) {
            throw new InsufficientBalanceException("Số dư không đủ. Bạn cần 5 điểm để đăng xe mới.");
        }

        wallet.setAvailablePoints(wallet.getAvailablePoints() - postFee);
        walletRepository.save(wallet);

        // Record the transaction
        PointTransaction tx = new PointTransaction();
        tx.setUser(seller);
        tx.setAmount(postFee);
        tx.setType(PointTransaction.TransactionType.SPEND);
        tx.setStatus(PointTransaction.TransactionStatus.SUCCESS);
        tx.setReferenceId("BIKE_POST_FEE_" + bike.getId());
        tx.setRemarks("Phí đăng tin xe đạp mới");
        pointTxRepo.save(tx);

        Bike saved = bikeRepository.save(bike);
        historyService.log("bike", saved.getId(), "created", seller.getId(), null);
        return saved;
    }

    public Bike updateBike(Long bikeId, Long sellerId, BikeCreateRequest request) {
        Bike bike = getBikeById(bikeId);

        if (!bike.getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("Only the seller can update this bike");
        }

        if (bike.getStatus() == Bike.BikeStatus.RESERVED || bike.getStatus() == Bike.BikeStatus.SOLD) {
            throw new IllegalStateException("Cannot update a bike that is reserved or sold");
        }

        bike.setTitle(request.getTitle());
        bike.setDescription(request.getDescription());

        Brand brandEntity = brandRepository.findByName(request.getBrand())
                .orElseGet(() -> {
                    Brand nb = new Brand();
                    nb.setName(request.getBrand());
                    return brandRepository.save(nb);
                });
        bike.setBrand(brandEntity);

        bike.setModel(request.getModel());
        bike.setYear(request.getYear());
        bike.setPricePoints(request.getPricePoints());
        bike.setCondition(request.getCondition());
        bike.setBikeType(request.getBikeType());
        bike.setFrameSize(request.getFrameSize());
        bike.setUpdatedAt(LocalDateTime.now());

        // Handle Media logic
        if (request.getMedia() != null) {
            bike.getMedia().clear(); // Orphan removal will delete old rows
            for (int i = 0; i < request.getMedia().size(); i++) {
                BikeMediaRequest mr = request.getMedia().get(i);
                BikeMedia bm = new BikeMedia();
                bm.setBike(bike);
                bm.setUrl(mr.getUrl());
                bm.setType(BikeMedia.MediaType.valueOf(mr.getType().toUpperCase()));
                bm.setSortOrder(mr.getSortOrder() != null ? mr.getSortOrder() : i);
                bike.getMedia().add(bm);
            }
        }

        // Handle Categories
        if (request.getCategoryIds() != null) {
            Set<Category> categories = new HashSet<>();
            for (Long cid : request.getCategoryIds()) {
                Category cat = categoryRepository.findById(cid)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + cid));
                categories.add(cat);
            }
            bike.setCategories(categories);
        }

        Bike saved = bikeRepository.save(bike);
        historyService.log("bike", saved.getId(), "updated", sellerId, null);
        return saved;
    }

    public void deleteBike(Long bikeId, Long sellerId) {
        Bike bike = getBikeById(bikeId);

        if (!bike.getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("Only the seller can delete this bike");
        }

        if (bike.getStatus() == Bike.BikeStatus.RESERVED) {
            throw new IllegalStateException("Cannot delete a reserved bike");
        }

        // Soft delete or just update status to CANCELLED
        bike.setStatus(Bike.BikeStatus.CANCELLED);
        bike.setUpdatedAt(LocalDateTime.now());
        bikeRepository.save(bike);
        historyService.log("bike", bike.getId(), "cancelled", sellerId, null);
    }

}
