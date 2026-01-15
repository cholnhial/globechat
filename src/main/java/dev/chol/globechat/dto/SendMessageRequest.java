package dev.chol.globechat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for sending a chat message.
 */
public record SendMessageRequest(
        @NotBlank(message = "Message content is required")
        @Size(max = 2000, message = "Message must be at most 2000 characters")
        String content
) {
}
