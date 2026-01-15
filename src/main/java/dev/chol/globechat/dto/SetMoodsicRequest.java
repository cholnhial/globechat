package dev.chol.globechat.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for setting room moodsic.
 */
public record SetMoodsicRequest(
        @NotNull(message = "Moodsic ID is required")
        Long moodsicId
) {
}
