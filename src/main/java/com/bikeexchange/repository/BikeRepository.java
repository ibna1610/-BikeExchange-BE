package com.bikeexchange.repository;

import com.bikeexchange.model.Bike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BikeRepository extends JpaRepository<Bike, Long> {
    Page<Bike> findByStatus(Bike.BikeStatus status, Pageable pageable);

    Page<Bike> findBySellerIdAndStatus(Long sellerId, Bike.BikeStatus status, Pageable pageable);

    Page<Bike> findBySellerId(Long sellerId, Pageable pageable);

    @Query("SELECT b FROM Bike b WHERE b.status = 'AVAILABLE' " +
           "AND (b.brand LIKE %:keyword% OR b.model LIKE %:keyword% OR b.title LIKE %:keyword%)")
    Page<Bike> searchAvailableBikes(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT b FROM Bike b WHERE b.status = 'AVAILABLE' " +
           "AND b.price BETWEEN :minPrice AND :maxPrice " +
           "AND b.year >= :minYear")
    Page<Bike> filterBikes(@Param("minPrice") Long minPrice,
                          @Param("maxPrice") Long maxPrice,
                          @Param("minYear") Integer minYear,
                          Pageable pageable);

    List<Bike> findByBikeTypeAndStatus(String bikeType, Bike.BikeStatus status);
}
