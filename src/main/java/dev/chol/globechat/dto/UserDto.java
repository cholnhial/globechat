package dev.chol.globechat.dto;

import dev.chol.globechat.entity.User;

import java.time.Instant;

/**
 * DTO for user information.
 */
public record UserDto(
        Long id,
        String username,
        String email,
        Instant createdAt
) {
    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }
}
