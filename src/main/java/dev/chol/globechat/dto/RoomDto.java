package dev.chol.globechat.dto;

import dev.chol.globechat.entity.ChatRoom;

import java.time.Instant;

/**
 * DTO for chat room information.
 */
public record RoomDto(
        Long id,
        String joinCode,
        String title,
        String description,
        String rules,
        UserDto owner,
        MoodsicDto currentMoodsic,
        boolean moodsicPaused,
        Double latitude,
        Double longitude,
        long memberCount,
        Instant createdAt
) {
    public static RoomDto from(ChatRoom room, long memberCount) {
        return new RoomDto(
                room.getId(),
                room.getJoinCode(),
                room.getTitle(),
                room.getDescription(),
                room.getRules(),
                UserDto.from(room.getOwner()),
                room.getCurrentMoodsic() != null ? MoodsicDto.from(room.getCurrentMoodsic()) : null,
                room.isMoodsicPaused(),
                room.getLatitude(),
                room.getLongitude(),
                memberCount,
                room.getCreatedAt()
        );
    }
}
