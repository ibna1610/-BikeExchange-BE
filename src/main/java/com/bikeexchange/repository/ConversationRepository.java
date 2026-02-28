package com.bikeexchange.repository;

import com.bikeexchange.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c WHERE c.buyer.id = :userId OR c.seller.id = :userId ORDER BY c.updatedAt DESC")
    List<Conversation> findUserConversations(@Param("userId") Long userId);

    @Query("SELECT c FROM Conversation c WHERE c.bike.id = :bikeId AND c.buyer.id = :buyerId")
    Optional<Conversation> findByBikeIdAndBuyerId(@Param("bikeId") Long bikeId,
            @Param("buyerId") Long buyerId);
}
