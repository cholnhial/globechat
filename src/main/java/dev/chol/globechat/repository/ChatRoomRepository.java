package dev.chol.globechat.repository;

import dev.chol.globechat.entity.ChatRoom;
import dev.chol.globechat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ChatRoom entities.
 */
@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * Find a chat room by its unique join code.
     */
    Optional<ChatRoom> findByJoinCode(String joinCode);

    /**
     * Find all rooms owned by a specific user.
     */
    List<ChatRoom> findByOwner(User owner);

    /**
     * Check if a join code already exists.
     */
    boolean existsByJoinCode(String joinCode);

    /**
     * Search rooms by title (case-insensitive).
     */
    @Query("SELECT r FROM ChatRoom r WHERE LOWER(r.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<ChatRoom> searchByTitle(@Param("query") String query);

    /**
     * Find rooms with a specific moodsic currently playing.
     */
    @Query("SELECT r FROM ChatRoom r WHERE r.currentMoodsic.id = :moodsicId")
    List<ChatRoom> findByCurrentMoodsicId(@Param("moodsicId") Long moodsicId);

    /**
     * Count the number of members in a room.
     */
    @Query("SELECT COUNT(m) FROM ChatRoomMember m WHERE m.chatRoom.id = :roomId")
    long countMembers(@Param("roomId") Long roomId);
}
