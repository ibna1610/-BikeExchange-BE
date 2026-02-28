package com.bikeexchange.service;

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

    @Transactional
    public Message sendMessage(Long senderId, MessageSendRequest request) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        Conversation conversation;

        if (request.getConversationId() != null) {
            conversation = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        } else {
            // Check if conversation already exists for this listing and buyer
            Optional<Conversation> existing = conversationRepository.findByListingIdAndBuyerId(request.getListingId(),
                    senderId);
            if (existing.isPresent()) {
                conversation = existing.get();
            } else {
                conversation = new Conversation();
                Bike listing = bikeRepository.findById(request.getListingId())
                        .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
                User receiver = userRepository.findById(request.getReceiverId())
                        .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

                conversation.setListing(listing);
                conversation.setBuyer(sender);
                conversation.setSeller(receiver);
                conversation.setCreatedAt(LocalDateTime.now());
            }
        }

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
