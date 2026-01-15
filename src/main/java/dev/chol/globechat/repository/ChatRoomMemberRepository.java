package dev.chol.globechat.repository;

import dev.chol.globechat.entity.ChatRoom;
import dev.chol.globechat.entity.ChatRoomMember;
import dev.chol.globechat.entity.ChatRoomMemberId;
import dev.chol.globechat.entity.MemberRole;
import dev.chol.globechat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ChatRoomMember entities.
 */
@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, ChatRoomMemberId> {

    /**
     * Find all memberships for a user (rooms they've joined).
     */
    List<ChatRoomMember> findByUser(User user);

    /**
     * Find all members of a chat room.
     */
    List<ChatRoomMember> findByChatRoom(ChatRoom chatRoom);

    /**
     * Find a specific membership by user and room.
     */
    Optional<ChatRoomMember> findByUserAndChatRoom(User user, ChatRoom chatRoom);

    /**
     * Find all members with a specific role in a room.
     */
    List<ChatRoomMember> findByChatRoomAndRole(ChatRoom chatRoom, MemberRole role);

    /**
     * Check if a user is a member of a room.
     */
    boolean existsByUserAndChatRoom(User user, ChatRoom chatRoom);

    /**
     * Find all rooms a user has joined.
     * Uses JOIN FETCH for lazy associations to support GraalVM native image.
     */
    @Query("SELECT DISTINCT m.chatRoom FROM ChatRoomMember m " +
           "JOIN FETCH m.chatRoom.owner " +
           "LEFT JOIN FETCH m.chatRoom.currentMoodsic " +
           "WHERE m.user = :user")
    List<ChatRoom> findRoomsByUser(@Param("user") User user);

    /**
     * Find all rooms where user has a specific role.
     * Uses JOIN FETCH for lazy associations to support GraalVM native image.
     */
    @Query("SELECT DISTINCT m.chatRoom FROM ChatRoomMember m " +
           "JOIN FETCH m.chatRoom.owner " +
           "LEFT JOIN FETCH m.chatRoom.currentMoodsic " +
           "WHERE m.user = :user AND m.role = :role")
    List<ChatRoom> findRoomsByUserAndRole(@Param("user") User user, @Param("role") MemberRole role);

    /**
     * Count members in a room.
     */
    long countByChatRoom(ChatRoom chatRoom);

    /**
     * Count mods in a room.
     */
    @Query("SELECT COUNT(m) FROM ChatRoomMember m WHERE m.chatRoom = :chatRoom AND m.role = 'MOD'")
    long countModsByChatRoom(@Param("chatRoom") ChatRoom chatRoom);
}
