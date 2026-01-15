package dev.chol.globechat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for ChatRoomMember entity.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomMemberId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "chat_room_id")
    private Long chatRoomId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatRoomMemberId that = (ChatRoomMemberId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(chatRoomId, that.chatRoomId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, chatRoomId);
    }
}
