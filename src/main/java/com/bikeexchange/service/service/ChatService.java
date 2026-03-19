package com.bikeexchange.service.service;

import com.bikeexchange.dto.request.ConversationCreateRequest;
import com.bikeexchange.dto.request.MessageSendRequest;
import com.bikeexchange.exception.ResourceNotFoundException;
import com.bikeexchange.model.Bike;
import com.bikeexchange.model.Conversation;
import com.bikeexchange.model.Message;
import com.bikeexchange.model.User;
import com.bikeexchange.repository.BikeRepository;
import com.bikeexchange.repository.ConversationRepository;
import com.bikeexchange.repository.MessageRepository;
import com.bikeexchange.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ChatService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BikeRepository bikeRepository;

    public List<Conversation> getUserConversations(Long userId) {
        return conversationRepository.findUserConversations(userId);
    }

    public Page<Message> getMessages(Long conversationId, Pageable pageable) {
        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
    }

    public List<Message> getMessagesAll(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    @Transactional
    public Conversation createConversation(Long buyerId, ConversationCreateRequest request) {
        if (request.getBikeId() == null) {
            throw new IllegalArgumentException("bikeId is required to start a conversation");
        }

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));

        // Check if conversation already exists for this listing and buyer
        Optional<Conversation> existing = conversationRepository.findByBikeIdAndBuyerId(request.getBikeId(),
                buyerId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Bike listing = bikeRepository.findById(request.getBikeId())
                .orElseThrow(() -> new ResourceNotFoundException("Bike listing not found"));

        if (listing.getDeletedAt() != null) {
            throw new IllegalStateException("Cannot start a conversation about a deleted bike");
        }

        User seller = listing.getSeller();
        if (seller == null) {
            throw new ResourceNotFoundException("Seller not found for this bike");
        }

        if (seller.getId().equals(buyerId)) {
            throw new IllegalArgumentException("Cannot start a conversation with yourself");
        }

        Conversation conversation = new Conversation();
        conversation.setBike(listing);
        conversation.setBuyer(buyer);
        conversation.setSeller(seller);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());

        return conversationRepository.save(conversation);
    }

    @Transactional
    public Message sendMessage(Long senderId, MessageSendRequest request) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        Conversation conversation;

        if (request.getConversationId() == null) {
            throw new IllegalArgumentException("conversationId is required to send a message");
        }

        conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        conversation.setUpdatedAt(LocalDateTime.now());
        conversation = conversationRepository.save(conversation);

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(request.getContent());
        message.setIsRead(false);
        message.setCreatedAt(LocalDateTime.now());

        return messageRepository.save(message);
    }

    @Transactional
    public void markAsRead(Long conversationId, Long userId) {
        List<Message> unread = messageRepository.findByConversationIdAndIsReadFalseAndSenderIdNot(conversationId,
                userId);
        unread.forEach(m -> m.setIsRead(true));
        messageRepository.saveAll(unread);
    }
}
