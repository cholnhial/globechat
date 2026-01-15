package dev.chol.globechat.service;

import dev.chol.globechat.controller.ChatWebSocketController;
import dev.chol.globechat.dto.*;
import dev.chol.globechat.entity.*;
import dev.chol.globechat.exception.BadRequestException;
import dev.chol.globechat.exception.ForbiddenException;
import dev.chol.globechat.exception.ResourceNotFoundException;
import dev.chol.globechat.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for chat room operations.
 */
@Service
@RequiredArgsConstructor
public class RoomService {

    private final ChatRoomRepository roomRepository;
    private final ChatRoomMemberRepository memberRepository;
    private final RoomBanRepository banRepository;
    private final MoodsicRepository moodsicRepository;
    private final UserService userService;
    @Lazy
    private final ChatWebSocketController webSocketController;

    /**
     * Create a new chat room.
     */
    @Transactional
    public RoomDto createRoom(CreateRoomRequest request) {
        User owner = userService.getCurrentUser();

        ChatRoom room = new ChatRoom(request.title(), request.description(), owner, request.latitude(), request.longitude());
        room.setRules(request.rules());
        room = roomRepository.save(room);

        // Add owner as first member with OWNER role
        ChatRoomMember ownerMember = new ChatRoomMember(owner, room, MemberRole.OWNER);
        memberRepository.save(ownerMember);

        return RoomDto.from(room, 1);
    }

    /**
     * Get a room by join code.
     */
    @Transactional(readOnly = true)
    public RoomDto getRoom(String joinCode) {
        ChatRoom room = findRoomByJoinCode(joinCode);
        long memberCount = memberRepository.countByChatRoom(room);
        return RoomDto.from(room, memberCount);
    }

    /**
     * Update a room (owner only).
     */
    @Transactional
    public RoomDto updateRoom(String joinCode, UpdateRoomRequest request) {
        User currentUser = userService.getCurrentUser();
        ChatRoom room = findRoomByJoinCode(joinCode);

        verifyOwner(room, currentUser);

        if (request.title() != null) {
            room.setTitle(request.title());
        }
        if (request.description() != null) {
            room.setDescription(request.description());
        }
        if (request.rules() != null) {
            room.setRules(request.rules());
        }

        room = roomRepository.save(room);
        long memberCount = memberRepository.countByChatRoom(room);
        return RoomDto.from(room, memberCount);
    }

    /**
     * Delete a room (owner only).
     */
    @Transactional
    public void deleteRoom(String joinCode) {
        User currentUser = userService.getCurrentUser();
        ChatRoom room = findRoomByJoinCode(joinCode);

        verifyOwner(room, currentUser);

        roomRepository.delete(room);
    }

    /**
     * Join a room.
     */
    @Transactional
    public RoomDto joinRoom(String joinCode) {
        User currentUser = userService.getCurrentUser();
        ChatRoom room = findRoomByJoinCode(joinCode);

        // Check if user is banned
        if (banRepository.existsByChatRoomAndBannedUser(room, currentUser)) {
            throw new ForbiddenException("You are banned from this room");
        }

        // Check if already a member
        if (memberRepository.existsByUserAndChatRoom(currentUser, room)) {
            throw new BadRequestException("You are already a member of this room");
        }

        ChatRoomMember member = new ChatRoomMember(currentUser, room, MemberRole.CHATTER);
        memberRepository.save(member);

        long memberCount = memberRepository.countByChatRoom(room);
        return RoomDto.from(room, memberCount);
    }

    /**
     * Leave a room.
     */
    @Transactional
    public void leaveRoom(String joinCode) {
        User currentUser = userService.getCurrentUser();
        ChatRoom room = findRoomByJoinCode(joinCode);

        // Owner cannot leave their room
        if (room.getOwner().equals(currentUser)) {
            throw new BadRequestException("Owner cannot leave the room. Transfer ownership or delete the room.");
        }

        ChatRoomMember membership = memberRepository.findByUserAndChatRoom(currentUser, room)
                .orElseThrow(() -> new BadRequestException("You are not a member of this room"));

        memberRepository.delete(membership);
    }

    /**
     * Get all members of a room.
     */
    @Transactional(readOnly = true)
    public List<RoomMemberDto> getMembers(String joinCode) {
        User currentUser = userService.getCurrentUser();
        ChatRoom room = findRoomByJoinCode(joinCode);

        // Verify user is a member
        verifyMembership(room, currentUser);

        return memberRepository.findByChatRoom(room).stream()
                .map(RoomMemberDto::from)
                .toList();
    }

    /**
     * Kick a user from the room.
     */
    @Transactional
    public void kickUser(String joinCode, String username) {
        User currentUser = userService.getCurrentUser();
        User targetUser = userService.findByUsername(username);
        ChatRoom room = findRoomByJoinCode(joinCode);

        verifyKickPermission(room, currentUser, targetUser);

        ChatRoomMember targetMembership = memberRepository.findByUserAndChatRoom(targetUser, room)
                .orElseThrow(() -> new BadRequestException("User is not a member of this room"));

        memberRepository.delete(targetMembership);

        // Broadcast kick notification to the room
        webSocketController.broadcastKick(joinCode, targetUser.getUsername(), currentUser.getUsername());
    }

    /**
     * Ban a user from the room.
     */
    @Transactional
    public void banUser(String joinCode, BanRequest request) {
        User currentUser = userService.getCurrentUser();
        User targetUser = userService.findByUsername(request.username());
        ChatRoom room = findRoomByJoinCode(joinCode);

        verifyKickPermission(room, currentUser, targetUser);

        // Remove membership if exists
        boolean wasMember = memberRepository.findByUserAndChatRoom(targetUser, room)
                .map(membership -> {
                    memberRepository.delete(membership);
                    return true;
                })
                .orElse(false);

        // Check if already banned
        if (banRepository.existsByChatRoomAndBannedUser(room, targetUser)) {
            throw new BadRequestException("User is already banned from this room");
        }

        RoomBan ban = new RoomBan(room, targetUser, currentUser, request.reason());
        banRepository.save(ban);

        // Broadcast ban notification to the room (only if they were a member)
        if (wasMember) {
            webSocketController.broadcastBan(joinCode, targetUser.getUsername(), currentUser.getUsername(), request.reason());
        }
    }

    /**
     * Unban a user from the room.
     */
    @Transactional
    public void unbanUser(String joinCode, String username) {
        User currentUser = userService.getCurrentUser();
        User targetUser = userService.findByUsername(username);
        ChatRoom room = findRoomByJoinCode(joinCode);

        verifyModeratorOrOwner(room, currentUser);

        RoomBan ban = banRepository.findByChatRoomAndBannedUser(room, targetUser)
                .orElseThrow(() -> new BadRequestException("User is not banned from this room"));

        banRepository.delete(ban);
    }

    /**
     * Promote a user to moderator (owner only).
     */
    @Transactional
    public void promoteMod(String joinCode, String username) {
        User currentUser = userService.getCurrentUser();
        User targetUser = userService.findByUsername(username);
        ChatRoom room = findRoomByJoinCode(joinCode);

        verifyOwner(room, currentUser);

        ChatRoomMember membership = memberRepository.findByUserAndChatRoom(targetUser, room)
                .orElseThrow(() -> new BadRequestException("User is not a member of this room"));

        if (membership.getRole() == MemberRole.OWNER) {
            throw new BadRequestException("Cannot change owner's role");
        }

        if (membership.getRole() == MemberRole.MOD) {
            throw new BadRequestException("User is already a moderator");
        }

        membership.setRole(MemberRole.MOD);
        memberRepository.save(membership);
    }

    /**
     * Demote a moderator to chatter (owner only).
     */
    @Transactional
    public void demoteMod(String joinCode, String username) {
        User currentUser = userService.getCurrentUser();
        User targetUser = userService.findByUsername(username);
        ChatRoom room = findRoomByJoinCode(joinCode);

        verifyOwner(room, currentUser);

        ChatRoomMember membership = memberRepository.findByUserAndChatRoom(targetUser, room)
                .orElseThrow(() -> new BadRequestException("User is not a member of this room"));

        if (membership.getRole() != MemberRole.MOD) {
            throw new BadRequestException("User is not a moderator");
        }

        membership.setRole(MemberRole.CHATTER);
        memberRepository.save(membership);
    }

    /**
     * Set the room's moodsic (owner only).
     */
    @Transactional
    public RoomDto setMoodsic(String joinCode, SetMoodsicRequest request) {
        User currentUser = userService.getCurrentUser();
        ChatRoom room = findRoomByJoinCode(joinCode);

        verifyOwner(room, currentUser);

        Moodsic moodsic = moodsicRepository.findById(request.moodsicId())
                .orElseThrow(() -> new ResourceNotFoundException("Moodsic", "id", request.moodsicId()));

        // Verify moodsic is accessible (public or owned by user)
        if (!moodsic.isPublic() && !moodsic.getUploadedBy().equals(currentUser)) {
            throw new ForbiddenException("You don't have access to this moodsic");
        }

        room.setCurrentMoodsic(moodsic);
        room = roomRepository.save(room);

        // Increment play count
        moodsicRepository.incrementPlayCount(moodsic.getId());

        long memberCount = memberRepository.countByChatRoom(room);
        return RoomDto.from(room, memberCount);
    }

    /**
     * Clear the room's moodsic (owner only).
     */
    @Transactional
    public RoomDto clearMoodsic(String joinCode) {
        User currentUser = userService.getCurrentUser();
        ChatRoom room = findRoomByJoinCode(joinCode);

        verifyOwner(room, currentUser);

        room.setCurrentMoodsic(null);
        room = roomRepository.save(room);

        long memberCount = memberRepository.countByChatRoom(room);
        return RoomDto.from(room, memberCount);
    }

    /**
     * Get rooms the current user has joined.
     */
    @Transactional(readOnly = true)
    public List<RoomDto> getMyRooms() {
        User currentUser = userService.getCurrentUser();
        List<ChatRoom> rooms = memberRepository.findRoomsByUser(currentUser);
        return rooms.stream()
                .map(room -> RoomDto.from(room, memberRepository.countByChatRoom(room)))
                .toList();
    }

    /**
     * Get all room markers for the globe.
     */
    @Transactional(readOnly = true)
    public List<RoomMarkerDto> getAllRoomMarkers() {
        return roomRepository.findAll().stream()
                .map(room -> RoomMarkerDto.from(room, memberRepository.countByChatRoom(room)))
                .toList();
    }

    /**
     * Toggle moodsic pause state (owner/mod only).
     */
    @Transactional
    public RoomDto toggleMoodsicPause(String joinCode) {
        User currentUser = userService.getCurrentUser();
        ChatRoom room = findRoomByJoinCode(joinCode);

        verifyModeratorOrOwner(room, currentUser);

        if (room.getCurrentMoodsic() == null) {
            throw new BadRequestException("No moodsic is currently set for this room");
        }

        room.setMoodsicPaused(!room.isMoodsicPaused());
        room = roomRepository.save(room);

        long memberCount = memberRepository.countByChatRoom(room);
        return RoomDto.from(room, memberCount);
    }

    /**
     * Get the user's role in a room.
     */
    @Transactional(readOnly = true)
    public MemberRole getUserRole(String joinCode) {
        User currentUser = userService.getCurrentUser();
        ChatRoom room = findRoomByJoinCode(joinCode);

        return memberRepository.findByUserAndChatRoom(currentUser, room)
                .map(ChatRoomMember::getRole)
                .orElse(null);
    }

    // Helper methods

    private ChatRoom findRoomByJoinCode(String joinCode) {
        return roomRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "joinCode", joinCode));
    }

    private void verifyOwner(ChatRoom room, User user) {
        if (!room.getOwner().equals(user)) {
            throw new ForbiddenException("Only the room owner can perform this action");
        }
    }

    private void verifyMembership(ChatRoom room, User user) {
        if (!memberRepository.existsByUserAndChatRoom(user, room)) {
            throw new ForbiddenException("You must be a member of this room");
        }
    }

    private void verifyModeratorOrOwner(ChatRoom room, User user) {
        if (room.getOwner().equals(user)) {
            return;
        }

        ChatRoomMember membership = memberRepository.findByUserAndChatRoom(user, room)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this room"));

        if (membership.getRole() != MemberRole.MOD && membership.getRole() != MemberRole.OWNER) {
            throw new ForbiddenException("Only moderators or owner can perform this action");
        }
    }

    private void verifyKickPermission(ChatRoom room, User kicker, User target) {
        // Cannot kick yourself
        if (kicker.equals(target)) {
            throw new BadRequestException("You cannot kick yourself");
        }

        // Owner can kick anyone
        if (room.getOwner().equals(kicker)) {
            return;
        }

        // Get kicker's role
        ChatRoomMember kickerMembership = memberRepository.findByUserAndChatRoom(kicker, room)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this room"));

        if (kickerMembership.getRole() != MemberRole.MOD) {
            throw new ForbiddenException("Only moderators or owner can kick users");
        }

        // Mods cannot kick other mods or the owner
        ChatRoomMember targetMembership = memberRepository.findByUserAndChatRoom(target, room).orElse(null);
        if (targetMembership != null && 
            (targetMembership.getRole() == MemberRole.MOD || targetMembership.getRole() == MemberRole.OWNER)) {
            throw new ForbiddenException("Moderators cannot kick other moderators or the owner");
        }
    }
}
