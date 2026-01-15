package dev.chol.globechat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a chat room.
 */
public record CreateRoomRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title must be at most 100 characters")
        String title,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        String rules,

        @NotNull(message = "Latitude is required")
        Double latitude,

        @NotNull(message = "Longitude is required")
        Double longitude
) {
}
