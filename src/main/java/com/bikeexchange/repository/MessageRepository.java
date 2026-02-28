package com.bikeexchange.repository;

import com.bikeexchange.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    List<Message> findByConversationIdAndIsReadFalseAndSenderIdNot(Long conversationId, Long userId);
}
