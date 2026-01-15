package dev.chol.globechat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a user's membership in a chat room.
 * Tracks the user's role (OWNER, MOD, CHATTER) and when they joined.
 * Uses composite primary key (userId, chatRoomId).
 */
@Entity
@Table(name = "chat_room_members")
@Getter
@Setter
@NoArgsConstructor
public class ChatRoomMember {

    @EmbeddedId
    private ChatRoomMemberId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("chatRoomId")
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @PrePersist
    protected void onCreate() {
        this.joinedAt = Instant.now();
    }

    public ChatRoomMember(User user, ChatRoom chatRoom, MemberRole role) {
        this.id = new ChatRoomMemberId(user.getId(), chatRoom.getId());
        this.user = user;
        this.chatRoom = chatRoom;
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatRoomMember that = (ChatRoomMember) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "ChatRoomMember{" +
                "userId=" + (id != null ? id.getUserId() : null) +
                ", chatRoomId=" + (id != null ? id.getChatRoomId() : null) +
                ", role=" + role +
                '}';
    }
}
