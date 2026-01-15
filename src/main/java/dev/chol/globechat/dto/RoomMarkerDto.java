package dev.chol.globechat.dto;

import dev.chol.globechat.entity.ChatRoom;

/**
 * DTO for room marker data on the globe (lightweight).
 */
public record RoomMarkerDto(
        String joinCode,
        String title,
        Double latitude,
        Double longitude,
        long memberCount
) {
    public static RoomMarkerDto from(ChatRoom room, long memberCount) {
        return new RoomMarkerDto(
                room.getJoinCode(),
                room.getTitle(),
                room.getLatitude(),
                room.getLongitude(),
                memberCount
        );
    }
}
