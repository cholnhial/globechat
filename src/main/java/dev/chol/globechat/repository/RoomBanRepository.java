package dev.chol.globechat.repository;

import dev.chol.globechat.entity.ChatRoom;
import dev.chol.globechat.entity.RoomBan;
import dev.chol.globechat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for RoomBan entities.
 */
@Repository
public interface RoomBanRepository extends JpaRepository<RoomBan, Long> {

    /**
     * Find a ban record for a specific user in a specific room.
     */
    Optional<RoomBan> findByChatRoomAndBannedUser(ChatRoom chatRoom, User bannedUser);

    /**
     * Check if a user is banned from a room.
     */
    boolean existsByChatRoomAndBannedUser(ChatRoom chatRoom, User bannedUser);

    /**
     * Find all bans in a specific room.
     */
    List<RoomBan> findByChatRoom(ChatRoom chatRoom);

    /**
     * Find all bans against a specific user (across all rooms).
     */
    List<RoomBan> findByBannedUser(User bannedUser);

    /**
     * Find all bans issued by a specific user.
     */
    List<RoomBan> findByBannedBy(User bannedBy);

    /**
     * Delete a ban (unban a user from a room).
     */
    void deleteByChatRoomAndBannedUser(ChatRoom chatRoom, User bannedUser);
}
