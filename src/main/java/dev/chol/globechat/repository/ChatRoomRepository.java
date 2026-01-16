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
 * Queries use JOIN FETCH for lazy associations to support GraalVM native image.
 */
@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * Find a chat room by its unique join code.
     * Uses JOIN FETCH for lazy associations to support GraalVM native image.
     */
    @Query("SELECT r FROM ChatRoom r " +
           "JOIN FETCH r.owner " +
           "LEFT JOIN FETCH r.currentMoodsic m " +
           "LEFT JOIN FETCH m.uploadedBy " +
           "WHERE r.joinCode = :joinCode")
    Optional<ChatRoom> findByJoinCode(@Param("joinCode") String joinCode);

    /**
     * Find all rooms owned by a specific user.
     * Uses JOIN FETCH for lazy associations to support GraalVM native image.
     */
    @Query("SELECT DISTINCT r FROM ChatRoom r " +
           "JOIN FETCH r.owner " +
           "LEFT JOIN FETCH r.currentMoodsic m " +
           "LEFT JOIN FETCH m.uploadedBy " +
           "WHERE r.owner = :owner")
    List<ChatRoom> findByOwner(@Param("owner") User owner);

    /**
     * Check if a join code already exists.
     */
    boolean existsByJoinCode(String joinCode);

    /**
     * Search rooms by title (case-insensitive).
     * Uses JOIN FETCH for lazy associations to support GraalVM native image.
     */
    @Query("SELECT DISTINCT r FROM ChatRoom r " +
           "JOIN FETCH r.owner " +
           "LEFT JOIN FETCH r.currentMoodsic m " +
           "LEFT JOIN FETCH m.uploadedBy " +
           "WHERE LOWER(r.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<ChatRoom> searchByTitle(@Param("query") String query);

    /**
     * Find rooms with a specific moodsic currently playing.
     * Uses JOIN FETCH for lazy associations to support GraalVM native image.
     */
    @Query("SELECT DISTINCT r FROM ChatRoom r " +
           "JOIN FETCH r.owner " +
           "LEFT JOIN FETCH r.currentMoodsic m " +
           "LEFT JOIN FETCH m.uploadedBy " +
           "WHERE r.currentMoodsic.id = :moodsicId")
    List<ChatRoom> findByCurrentMoodsicId(@Param("moodsicId") Long moodsicId);

    /**
     * Count the number of members in a room.
     */
    @Query("SELECT COUNT(m) FROM ChatRoomMember m WHERE m.chatRoom.id = :roomId")
    long countMembers(@Param("roomId") Long roomId);

    /**
     * Find a chat room by ID with all associations fetched.
     * Uses JOIN FETCH for lazy associations to support GraalVM native image.
     */
    @Query("SELECT r FROM ChatRoom r " +
           "JOIN FETCH r.owner " +
           "LEFT JOIN FETCH r.currentMoodsic m " +
           "LEFT JOIN FETCH m.uploadedBy " +
           "WHERE r.id = :id")
    Optional<ChatRoom> findByIdWithAssociations(@Param("id") Long id);

    /**
     * Find all chat rooms with all associations fetched.
     * Uses JOIN FETCH for lazy associations to support GraalVM native image.
     */
    @Query("SELECT DISTINCT r FROM ChatRoom r " +
           "JOIN FETCH r.owner " +
           "LEFT JOIN FETCH r.currentMoodsic m " +
           "LEFT JOIN FETCH m.uploadedBy")
    List<ChatRoom> findAllWithAssociations();
}
