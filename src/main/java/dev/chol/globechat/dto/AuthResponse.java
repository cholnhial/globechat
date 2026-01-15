package dev.chol.globechat.dto;

/**
 * Response DTO for authentication (login/register).
 */
public record AuthResponse(
        String token,
        UserDto user
) {
}
