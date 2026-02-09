package com.bikeexchange.repository;

import com.bikeexchange.model.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByBuyerId(Long buyerId);
    Optional<Wishlist> findByBuyerIdAndBikeId(Long buyerId, Long bikeId);
    Long countByBuyerId(Long buyerId);
    void deleteByBuyerIdAndBikeId(Long buyerId, Long bikeId);
}
