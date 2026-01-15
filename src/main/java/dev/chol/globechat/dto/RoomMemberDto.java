package dev.chol.globechat.dto;

import dev.chol.globechat.entity.ChatRoomMember;
import dev.chol.globechat.entity.MemberRole;

import java.time.Instant;

/**
 * DTO for room member information.
 */
public record RoomMemberDto(
        UserDto user,
        MemberRole role,
        Instant joinedAt
) {
    public static RoomMemberDto from(ChatRoomMember member) {
        return new RoomMemberDto(
                UserDto.from(member.getUser()),
                member.getRole(),
                member.getJoinedAt()
        );
    }
}
