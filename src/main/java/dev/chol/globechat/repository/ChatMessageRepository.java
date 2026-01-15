package dev.chol.globechat.repository;

import dev.chol.globechat.entity.ChatMessage;
import dev.chol.globechat.entity.ChatRoom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ChatMessage entities.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Find all messages in a room ordered by creation time.
     */
    List<ChatMessage> findByChatRoomOrderByCreatedAtAsc(ChatRoom chatRoom);

    /**
     * Find recent messages in a room (paginated).
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.chatRoom = :room ORDER BY m.createdAt DESC")
    List<ChatMessage> findRecentMessages(@Param("room") ChatRoom room, Pageable pageable);

    /**
     * Find the last N messages in a room.
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.chatRoom = :room ORDER BY m.createdAt DESC")
    List<ChatMessage> findLastNMessages(@Param("room") ChatRoom room, Pageable pageable);

    /**
     * Delete all messages in a room.
     */
    void deleteByChatRoom(ChatRoom chatRoom);

    /**
     * Count messages in a room.
     */
    long countByChatRoom(ChatRoom chatRoom);
}
