package dev.chol.globechat.dto;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a chat room.
 */
public record UpdateRoomRequest(
        @Size(max = 100, message = "Title must be at most 100 characters")
        String title,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        String rules
) {
}
