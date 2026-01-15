package dev.chol.globechat.dto;

import dev.chol.globechat.entity.ChatMessage;
import dev.chol.globechat.entity.MessageType;

import java.time.Instant;

/**
 * DTO for chat messages.
 */
public record ChatMessageDto(
        Long id,
        String content,
        MessageType type,
        UserDto sender,
        String roomJoinCode,
        Instant createdAt,
        String targetUsername
) {
    public static ChatMessageDto from(ChatMessage message) {
        return new ChatMessageDto(
                message.getId(),
                message.getContent(),
                message.getMessageType(),
                UserDto.from(message.getSender()),
                message.getChatRoom().getJoinCode(),
                message.getCreatedAt(),
                null
        );
    }

    /**
     * Create a system message DTO (no ID, no sender).
     */
    public static ChatMessageDto systemMessage(String content, MessageType type, String roomJoinCode) {
        return new ChatMessageDto(
                null,
                content,
                type,
                null,
                roomJoinCode,
                Instant.now(),
                null
        );
    }

    /**
     * Create a kick/ban message DTO with target username.
     */
    public static ChatMessageDto kickMessage(String content, MessageType type, String roomJoinCode, String targetUsername) {
        return new ChatMessageDto(
                null,
                content,
                type,
                null,
                roomJoinCode,
                Instant.now(),
                targetUsername
        );
    }
}
