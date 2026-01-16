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
 * Queries use JOIN FETCH for lazy associations to support GraalVM native image.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Find all messages in a room ordered by creation time.
     * Uses JOIN FETCH for lazy associations to support GraalVM native image.
     */
    @Query("SELECT m FROM ChatMessage m " +
           "JOIN FETCH m.sender " +
           "JOIN FETCH m.chatRoom cr " +
           "JOIN FETCH cr.owner " +
           "WHERE m.chatRoom = :chatRoom " +
           "ORDER BY m.createdAt ASC")
    List<ChatMessage> findByChatRoomOrderByCreatedAtAsc(@Param("chatRoom") ChatRoom chatRoom);

    /**
     * Find recent messages in a room (paginated).
     * Uses JOIN FETCH for lazy associations to support GraalVM native image.
     */
    @Query("SELECT m FROM ChatMessage m " +
           "JOIN FETCH m.sender " +
           "JOIN FETCH m.chatRoom cr " +
           "JOIN FETCH cr.owner " +
           "WHERE m.chatRoom = :room " +
           "ORDER BY m.createdAt DESC")
    List<ChatMessage> findRecentMessages(@Param("room") ChatRoom room, Pageable pageable);

    /**
     * Find the last N messages in a room.
     * Uses JOIN FETCH for lazy associations to support GraalVM native image.
     */
    @Query("SELECT m FROM ChatMessage m " +
           "JOIN FETCH m.sender " +
           "JOIN FETCH m.chatRoom cr " +
           "JOIN FETCH cr.owner " +
           "WHERE m.chatRoom = :room " +
           "ORDER BY m.createdAt DESC")
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
