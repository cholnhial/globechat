package dev.chol.globechat.repository;

import dev.chol.globechat.entity.ChatRoom;
import dev.chol.globechat.entity.RoomBan;
import dev.chol.globechat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for RoomBan entities.
 * Queries use JOIN FETCH for lazy associations to support GraalVM native image.
 */
@Repository
public interface RoomBanRepository extends JpaRepository<RoomBan, Long> {

    /**
     * Find a ban record for a specific user in a specific room.
     * Uses JOIN FETCH for lazy associations to support GraalVM native image.
     */
    @Query("SELECT b FROM RoomBan b " +
           "JOIN FETCH b.chatRoom cr " +
           "JOIN FETCH cr.owner " +
           "LEFT JOIN FETCH cr.currentMoodsic cm " +
           "LEFT JOIN FETCH cm.uploadedBy " +
           "JOIN FETCH b.bannedUser " +
           "JOIN FETCH b.bannedBy " +
           "WHERE b.chatRoom = :chatRoom AND b.bannedUser = :bannedUser")
    Optional<RoomBan> findByChatRoomAndBannedUser(@Param("chatRoom") ChatRoom chatRoom, @Param("bannedUser") User bannedUser);

    /**
     * Check if a user is banned from a room.
     */
    boolean existsByChatRoomAndBannedUser(ChatRoom chatRoom, User bannedUser);

    /**
     * Find all bans in a specific room.
     * Uses JOIN FETCH for lazy associations to support GraalVM native image.
     */
    @Query("SELECT b FROM RoomBan b " +
           "JOIN FETCH b.chatRoom cr " +
           "JOIN FETCH cr.owner " +
           "LEFT JOIN FETCH cr.currentMoodsic cm " +
           "LEFT JOIN FETCH cm.uploadedBy " +
           "JOIN FETCH b.bannedUser " +
           "JOIN FETCH b.bannedBy " +
           "WHERE b.chatRoom = :chatRoom")
    List<RoomBan> findByChatRoom(@Param("chatRoom") ChatRoom chatRoom);

    /**
     * Find all bans against a specific user (across all rooms).
     * Uses JOIN FETCH for lazy associations to support GraalVM native image.
     */
    @Query("SELECT b FROM RoomBan b " +
           "JOIN FETCH b.chatRoom cr " +
           "JOIN FETCH cr.owner " +
           "LEFT JOIN FETCH cr.currentMoodsic cm " +
           "LEFT JOIN FETCH cm.uploadedBy " +
           "JOIN FETCH b.bannedUser " +
           "JOIN FETCH b.bannedBy " +
           "WHERE b.bannedUser = :bannedUser")
    List<RoomBan> findByBannedUser(@Param("bannedUser") User bannedUser);

    /**
     * Find all bans issued by a specific user.
     * Uses JOIN FETCH for lazy associations to support GraalVM native image.
     */
    @Query("SELECT b FROM RoomBan b " +
           "JOIN FETCH b.chatRoom cr " +
           "JOIN FETCH cr.owner " +
           "LEFT JOIN FETCH cr.currentMoodsic cm " +
           "LEFT JOIN FETCH cm.uploadedBy " +
           "JOIN FETCH b.bannedUser " +
           "JOIN FETCH b.bannedBy " +
           "WHERE b.bannedBy = :bannedBy")
    List<RoomBan> findByBannedBy(@Param("bannedBy") User bannedBy);

    /**
     * Delete a ban (unban a user from a room).
     */
    void deleteByChatRoomAndBannedUser(ChatRoom chatRoom, User bannedUser);
}
