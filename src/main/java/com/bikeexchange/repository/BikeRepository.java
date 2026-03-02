package com.bikeexchange.repository;

import com.bikeexchange.model.Bike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

@Repository
public interface BikeRepository extends JpaRepository<Bike, Long> {
       @Lock(LockModeType.PESSIMISTIC_WRITE)
       @Query("SELECT b FROM Bike b WHERE b.id = :id")
       Optional<Bike> findByIdForUpdate(@Param("id") Long id);

       Page<Bike> findByStatus(Bike.BikeStatus status, Pageable pageable);

       Page<Bike> findBySellerIdAndStatus(Long sellerId, Bike.BikeStatus status, Pageable pageable);

       Page<Bike> findBySellerId(Long sellerId, Pageable pageable);

       Page<Bike> findBySellerIdAndStatusIn(Long sellerId, java.util.List<Bike.BikeStatus> statuses, Pageable pageable);

       @Query("SELECT b FROM Bike b WHERE (b.status = 'ACTIVE' OR b.status = 'VERIFIED') " +
                     "AND (b.brand.name LIKE %:keyword% OR b.model LIKE %:keyword% OR b.title LIKE %:keyword%)")
       Page<Bike> searchAvailableBikes(@Param("keyword") String keyword, Pageable pageable);

       @Query("SELECT b FROM Bike b WHERE (b.status = 'ACTIVE' OR b.status = 'VERIFIED') " +
                     "AND b.pricePoints BETWEEN :minPrice AND :maxPrice " +
                     "AND b.year >= :minYear")
       Page<Bike> filterBikes(@Param("minPrice") Long minPrice,
                     @Param("maxPrice") Long maxPrice,
                     @Param("minYear") Integer minYear,
                     Pageable pageable);

       @Query("SELECT b FROM Bike b WHERE (b.status = 'ACTIVE' OR b.status = 'VERIFIED') " +
                     "AND (:minPrice IS NULL OR b.pricePoints >= :minPrice) " +
                     "AND (:maxPrice IS NULL OR b.pricePoints <= :maxPrice) " +
                     "AND (:minYear IS NULL OR b.year >= :minYear) " +
                     "AND (:frameSize IS NULL OR b.frameSize = :frameSize)")
       Page<Bike> filterBikesAdvanced(@Param("minPrice") Long minPrice,
                     @Param("maxPrice") Long maxPrice,
                     @Param("minYear") Integer minYear,
                     @Param("frameSize") String frameSize,
                     Pageable pageable);

       List<Bike> findByBikeTypeAndStatus(String bikeType, Bike.BikeStatus status);

       Page<Bike> findByCategories_Id(Long categoryId, Pageable pageable);

       Page<Bike> findByCategories_IdAndStatus(Long categoryId, Bike.BikeStatus status, Pageable pageable);

       @Query("SELECT b FROM Bike b WHERE (b.brand.name LIKE %:keyword% OR b.model LIKE %:keyword% OR b.title LIKE %:keyword%)")
       Page<Bike> searchAllStatuses(@Param("keyword") String keyword, Pageable pageable);

       Page<Bike> findByStatusIn(java.util.List<Bike.BikeStatus> statuses, Pageable pageable);

       Page<Bike> findByCategories_IdAndStatusIn(Long categoryId, java.util.List<Bike.BikeStatus> statuses, Pageable pageable);

       @Query("SELECT b FROM Bike b WHERE b.status IN :statuses AND (" +
               "LOWER(b.brand.name) LIKE %:keyword% OR LOWER(b.model) LIKE %:keyword% OR LOWER(b.title) LIKE %:keyword%)")
       Page<Bike> searchByKeywordAndStatuses(@Param("keyword") String keyword,
                                             @Param("statuses") java.util.List<Bike.BikeStatus> statuses,
                                             Pageable pageable);

       @Query("SELECT b FROM Bike b JOIN b.categories c WHERE c.id = :categoryId AND b.status IN :statuses AND (" +
               "LOWER(b.brand.name) LIKE %:keyword% OR LOWER(b.model) LIKE %:keyword% OR LOWER(b.title) LIKE %:keyword%)")
       Page<Bike> searchByCategoryKeywordAndStatuses(@Param("categoryId") Long categoryId,
                                                     @Param("keyword") String keyword,
                                                     @Param("statuses") java.util.List<Bike.BikeStatus> statuses,
                                                     Pageable pageable);
}
