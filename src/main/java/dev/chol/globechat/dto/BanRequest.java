package dev.chol.globechat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for banning a user from a room.
 */
public record BanRequest(
        @NotBlank(message = "Username is required")
        String username,

        @Size(max = 500, message = "Reason must be at most 500 characters")
        String reason
) {
}
