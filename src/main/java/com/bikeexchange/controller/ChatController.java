package com.bikeexchange.controller;

import com.bikeexchange.dto.request.ConversationCreateRequest;
import com.bikeexchange.dto.request.MessageSendRequest;
import com.bikeexchange.model.Conversation;
import com.bikeexchange.model.Message;
import com.bikeexchange.security.UserPrincipal;
import com.bikeexchange.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/chat")
@Tag(name = "Chat & Messaging", description = "APIs for user conversations and messages. Supports both REST and WebSocket (STOMP)")
@SecurityRequirement(name = "Bearer Token")
public class ChatController {

        @Autowired
        private ChatService chatService;

        @Autowired
        private SimpMessagingTemplate messagingTemplate;

        @GetMapping("/conversations")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Get User Conversations", description = "Retrieve a list of all chat conversations involving the authenticated user")
        public ResponseEntity<?> getUserConversations(
                        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
                List<Conversation> conversations = chatService.getUserConversations(currentUser.getId());

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", conversations);

                return ResponseEntity.ok(response);
        }

        @PostMapping("/conversations")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Create or Get Conversation", description = "Explicitly creates a new conversation for a listing or returns an existing one.")
        public ResponseEntity<?> createConversation(
                        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
                        @RequestBody ConversationCreateRequest request) {
                Conversation conversation = chatService.createConversation(currentUser.getId(), request);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", conversation);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/conversations/{conversationId}/messages")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Get Messages in a Conversation", description = "Retrieve a paginated list of messages for a specific conversation. Also marks messages as read.")
        public ResponseEntity<?> getMessages(
                        @Parameter(description = "ID of the conversation", example = "1") @PathVariable Long conversationId,
                        @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "Number of items per page", example = "50") @RequestParam(defaultValue = "50") int size,
                        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser) {
                Pageable pageable = PageRequest.of(page, size);
                Page<Message> messages = chatService.getMessages(conversationId, pageable);

                // Mark as read when fetching
                chatService.markAsRead(conversationId, currentUser.getId());

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", messages);

                return ResponseEntity.ok(response);
        }

        // REST endpoint for sending messages
        @PostMapping("/messages")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Send a Message via HTTP POST", description = "Allows sending a chat message using standard HTTP POST. Useful for clients not using WebSockets.")
        public ResponseEntity<?> sendHttpMessage(
                        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal currentUser,
                        @RequestBody MessageSendRequest request) {

                Message savedMessage = chatService.sendMessage(currentUser.getId(), request);

                // Optionally broadcast to receiver's specific queue
                Long receiverId = savedMessage.getConversation().getBuyer().getId().equals(currentUser.getId())
                                ? savedMessage.getConversation().getSeller().getId()
                                : savedMessage.getConversation().getBuyer().getId();

                messagingTemplate.convertAndSendToUser(
                                receiverId.toString(),
                                "/queue/messages",
                                savedMessage);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", savedMessage);
                return ResponseEntity.ok(response);
        }

        // WebSocket STOMP endpoint
        @MessageMapping("/chat.sendMessage")
        @Operation(summary = "Send a Message via STOMP WebSocket", description = "Internal STOMP endpoint handling real-time WebSockets messages. Returns no HTTP response.")
        public void sendMessage(@Payload MessageSendRequest request, Principal principal) {
                // Retrieve sender ID from STOMP authenticated principal
                if (principal instanceof UsernamePasswordAuthenticationToken) {
                        UserPrincipal userPrincipal = (UserPrincipal) ((UsernamePasswordAuthenticationToken) principal)
                                        .getPrincipal();
                        Long senderId = userPrincipal.getId();

                        // Save to DB
                        Message savedMessage = chatService.sendMessage(senderId, request);

                        // Determine receiver (either from request if new convo, or from savedMessage
                        // conversation)
                        Long receiverId = savedMessage.getConversation().getBuyer().getId().equals(senderId)
                                        ? savedMessage.getConversation().getSeller().getId()
                                        : savedMessage.getConversation().getBuyer().getId();

                        // Broadcast to receiver's specific queue
                        messagingTemplate.convertAndSendToUser(
                                        receiverId.toString(),
                                        "/queue/messages",
                                        savedMessage);

                        // Optionally, also broadcast back to sender's queue for confirmation
                        messagingTemplate.convertAndSendToUser(
                                        senderId.toString(),
                                        "/queue/messages",
                                        savedMessage);
                }
        }
}
