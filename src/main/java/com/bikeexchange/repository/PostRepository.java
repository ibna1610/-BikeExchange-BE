package com.bikeexchange.repository;

import com.bikeexchange.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findBySellerId(Long sellerId, Pageable pageable);

    Page<Post> findBySellerIdAndStatusIn(Long sellerId, java.util.List<Post.PostStatus> statuses, Pageable pageable);

    Page<Post> findByStatusIn(java.util.List<Post.PostStatus> statuses, Pageable pageable);

    boolean existsByBikeIdAndStatusIn(Long bikeId, java.util.List<Post.PostStatus> statuses);
}
