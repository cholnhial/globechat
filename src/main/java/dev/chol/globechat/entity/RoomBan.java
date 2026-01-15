package dev.chol.globechat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a ban record for a user in a chat room.
 * Tracks who was banned, who issued the ban, and the reason.
 */
@Entity
@Table(name = "room_bans", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"chat_room_id", "banned_user_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class RoomBan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banned_user_id", nullable = false)
    private User bannedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banned_by_id", nullable = false)
    private User bannedBy;

    @Column(length = 500)
    private String reason;

    @Column(name = "banned_at", nullable = false, updatable = false)
    private Instant bannedAt;

    @PrePersist
    protected void onCreate() {
        this.bannedAt = Instant.now();
    }

    public RoomBan(ChatRoom chatRoom, User bannedUser, User bannedBy, String reason) {
        this.chatRoom = chatRoom;
        this.bannedUser = bannedUser;
        this.bannedBy = bannedBy;
        this.reason = reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoomBan roomBan = (RoomBan) o;
        return id != null && id.equals(roomBan.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "RoomBan{" +
                "id=" + id +
                ", reason='" + reason + '\'' +
                '}';
    }
}
