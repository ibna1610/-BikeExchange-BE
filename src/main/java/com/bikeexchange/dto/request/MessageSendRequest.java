package com.bikeexchange.dto.request;

import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Request payload for sending a chat message")
public class MessageSendRequest {

    @Schema(description = "ID of the existing conversation. If null, a new conversation will be created.", example = "1", nullable = true)
    private Long conversationId;

    @Schema(description = "ID of the associated bike listing. Required if creating a new conversation.", example = "10")
    private Long listingId;

    @Schema(description = "ID of the receiving user. Required if creating a new conversation.", example = "2")
    private Long receiverId;

    @Schema(description = "Content of the message", example = "Chào bạn, xe này còn thương lượng được không ạ?")
    private String content;
}
