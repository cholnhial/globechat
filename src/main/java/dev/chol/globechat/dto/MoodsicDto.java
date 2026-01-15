package dev.chol.globechat.dto;

import dev.chol.globechat.entity.Moodsic;

import java.time.Instant;

/**
 * DTO for moodsic information.
 */
public record MoodsicDto(
        Long id,
        String name,
        String contentType,
        boolean isPublic,
        Long playCount,
        UserDto uploadedBy,
        Instant createdAt
) {
    public static MoodsicDto from(Moodsic moodsic) {
        return new MoodsicDto(
                moodsic.getId(),
                moodsic.getName(),
                moodsic.getContentType(),
                moodsic.isPublic(),
                moodsic.getPlayCount(),
                UserDto.from(moodsic.getUploadedBy()),
                moodsic.getCreatedAt()
        );
    }
}
