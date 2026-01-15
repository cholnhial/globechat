package dev.chol.globechat.service;

import dev.chol.globechat.dto.*;
import dev.chol.globechat.entity.*;
import dev.chol.globechat.exception.BadRequestException;
import dev.chol.globechat.exception.ForbiddenException;
import dev.chol.globechat.exception.ResourceNotFoundException;
import dev.chol.globechat.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private ChatRoomRepository roomRepository;

    @Mock
    private ChatRoomMemberRepository memberRepository;

    @Mock
    private RoomBanRepository banRepository;

    @Mock
    private MoodsicRepository moodsicRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private RoomService roomService;

    private User owner;
    private User member;
    private User nonMember;
    private ChatRoom room;

    @BeforeEach
    void setUp() {
        owner = new User("owner", "owner@example.com", "password");
        owner.setId(1L);

        member = new User("member", "member@example.com", "password");
        member.setId(2L);

        nonMember = new User("nonmember", "nonmember@example.com", "password");
        nonMember.setId(3L);

        room = new ChatRoom("Test Room", "Description", owner, 40.7128, -74.0060);
        room.setId(1L);
        room.setJoinCode("TESTCODE");
    }

    @Test
    void createRoom_createsRoomAndAddOwnerAsMember() {
        CreateRoomRequest request = new CreateRoomRequest("Test Room", "Description", "Rules", 40.7128, -74.0060);
        when(userService.getCurrentUser()).thenReturn(owner);
        when(roomRepository.save(any(ChatRoom.class))).thenReturn(room);
        when(memberRepository.save(any(ChatRoomMember.class))).thenReturn(null);

        RoomDto result = roomService.createRoom(request);

        assertThat(result.title()).isEqualTo("Test Room");
        verify(roomRepository).save(any(ChatRoom.class));
        verify(memberRepository).save(argThat(m -> m.getRole() == MemberRole.OWNER));
    }

    @Test
    void getRoom_whenExists_returnsRoom() {
        when(roomRepository.findByJoinCode("TESTCODE")).thenReturn(Optional.of(room));
        when(memberRepository.countByChatRoom(room)).thenReturn(5L);

        RoomDto result = roomService.getRoom("TESTCODE");

        assertThat(result.joinCode()).isEqualTo("TESTCODE");
        assertThat(result.memberCount()).isEqualTo(5);
    }

    @Test
    void getRoom_whenNotExists_throwsException() {
        when(roomRepository.findByJoinCode("NOTEXIST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getRoom("NOTEXIST"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRoom_byOwner_deletesRoom() {
        when(userService.getCurrentUser()).thenReturn(owner);
        when(roomRepository.findByJoinCode("TESTCODE")).thenReturn(Optional.of(room));

        roomService.deleteRoom("TESTCODE");

        verify(roomRepository).delete(room);
    }

    @Test
    void deleteRoom_byNonOwner_throwsException() {
        when(userService.getCurrentUser()).thenReturn(member);
        when(roomRepository.findByJoinCode("TESTCODE")).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.deleteRoom("TESTCODE"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("owner");
    }

    @Test
    void joinRoom_whenNotBanned_addsMember() {
        when(userService.getCurrentUser()).thenReturn(nonMember);
        when(roomRepository.findByJoinCode("TESTCODE")).thenReturn(Optional.of(room));
        when(banRepository.existsByChatRoomAndBannedUser(room, nonMember)).thenReturn(false);
        when(memberRepository.existsByUserAndChatRoom(nonMember, room)).thenReturn(false);
        when(memberRepository.save(any(ChatRoomMember.class))).thenReturn(null);
        when(memberRepository.countByChatRoom(room)).thenReturn(2L);

        RoomDto result = roomService.joinRoom("TESTCODE");

        verify(memberRepository).save(argThat(m -> m.getRole() == MemberRole.CHATTER));
    }

    @Test
    void joinRoom_whenBanned_throwsException() {
        when(userService.getCurrentUser()).thenReturn(nonMember);
        when(roomRepository.findByJoinCode("TESTCODE")).thenReturn(Optional.of(room));
        when(banRepository.existsByChatRoomAndBannedUser(room, nonMember)).thenReturn(true);

        assertThatThrownBy(() -> roomService.joinRoom("TESTCODE"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("banned");
    }

    @Test
    void joinRoom_whenAlreadyMember_throwsException() {
        when(userService.getCurrentUser()).thenReturn(member);
        when(roomRepository.findByJoinCode("TESTCODE")).thenReturn(Optional.of(room));
        when(banRepository.existsByChatRoomAndBannedUser(room, member)).thenReturn(false);
        when(memberRepository.existsByUserAndChatRoom(member, room)).thenReturn(true);

        assertThatThrownBy(() -> roomService.joinRoom("TESTCODE"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already a member");
    }

    @Test
    void leaveRoom_whenOwner_throwsException() {
        when(userService.getCurrentUser()).thenReturn(owner);
        when(roomRepository.findByJoinCode("TESTCODE")).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.leaveRoom("TESTCODE"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Owner cannot leave");
    }

    @Test
    void kickUser_byOwner_kicksUser() {
        ChatRoomMember memberMembership = new ChatRoomMember(member, room, MemberRole.CHATTER);

        when(userService.getCurrentUser()).thenReturn(owner);
        when(userService.findByUsername("member")).thenReturn(member);
        when(roomRepository.findByJoinCode("TESTCODE")).thenReturn(Optional.of(room));
        when(memberRepository.findByUserAndChatRoom(member, room)).thenReturn(Optional.of(memberMembership));

        roomService.kickUser("TESTCODE", "member");

        verify(memberRepository).delete(memberMembership);
    }

    @Test
    void kickUser_modKickingMod_throwsException() {
        User mod1 = new User("mod1", "mod1@example.com", "password");
        mod1.setId(4L);
        User mod2 = new User("mod2", "mod2@example.com", "password");
        mod2.setId(5L);

        ChatRoomMember mod1Membership = new ChatRoomMember(mod1, room, MemberRole.MOD);
        ChatRoomMember mod2Membership = new ChatRoomMember(mod2, room, MemberRole.MOD);

        when(userService.getCurrentUser()).thenReturn(mod1);
        when(userService.findByUsername("mod2")).thenReturn(mod2);
        when(roomRepository.findByJoinCode("TESTCODE")).thenReturn(Optional.of(room));
        when(memberRepository.findByUserAndChatRoom(mod1, room)).thenReturn(Optional.of(mod1Membership));
        when(memberRepository.findByUserAndChatRoom(mod2, room)).thenReturn(Optional.of(mod2Membership));

        assertThatThrownBy(() -> roomService.kickUser("TESTCODE", "mod2"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("cannot kick other moderators");
    }

    @Test
    void banUser_createsBanRecord() {
        BanRequest request = new BanRequest("member", "Breaking rules");
        ChatRoomMember memberMembership = new ChatRoomMember(member, room, MemberRole.CHATTER);

        when(userService.getCurrentUser()).thenReturn(owner);
        when(userService.findByUsername("member")).thenReturn(member);
        when(roomRepository.findByJoinCode("TESTCODE")).thenReturn(Optional.of(room));
        when(memberRepository.findByUserAndChatRoom(member, room)).thenReturn(Optional.of(memberMembership));
        when(banRepository.existsByChatRoomAndBannedUser(room, member)).thenReturn(false);

        roomService.banUser("TESTCODE", request);

        verify(memberRepository).delete(memberMembership);
        verify(banRepository).save(argThat(ban -> ban.getReason().equals("Breaking rules")));
    }

    @Test
    void promoteMod_byOwner_promotesMember() {
        ChatRoomMember memberMembership = new ChatRoomMember(member, room, MemberRole.CHATTER);

        when(userService.getCurrentUser()).thenReturn(owner);
        when(userService.findByUsername("member")).thenReturn(member);
        when(roomRepository.findByJoinCode("TESTCODE")).thenReturn(Optional.of(room));
        when(memberRepository.findByUserAndChatRoom(member, room)).thenReturn(Optional.of(memberMembership));
        when(memberRepository.save(any(ChatRoomMember.class))).thenReturn(memberMembership);

        roomService.promoteMod("TESTCODE", "member");

        verify(memberRepository).save(argThat(m -> m.getRole() == MemberRole.MOD));
    }

    @Test
    void setMoodsic_byOwner_setsMoodsic() {
        SetMoodsicRequest request = new SetMoodsicRequest(1L);
        Moodsic moodsic = new Moodsic("Song", "/path", "audio/mpeg", owner, true);
        moodsic.setId(1L);

        when(userService.getCurrentUser()).thenReturn(owner);
        when(roomRepository.findByJoinCode("TESTCODE")).thenReturn(Optional.of(room));
        when(moodsicRepository.findById(1L)).thenReturn(Optional.of(moodsic));
        when(roomRepository.save(any(ChatRoom.class))).thenReturn(room);
        when(memberRepository.countByChatRoom(room)).thenReturn(1L);

        roomService.setMoodsic("TESTCODE", request);

        verify(moodsicRepository).incrementPlayCount(1L);
        verify(roomRepository).save(argThat(r -> r.getCurrentMoodsic() == moodsic));
    }

    @Test
    void setMoodsic_withPrivateMoodsicNotOwned_throwsException() {
        SetMoodsicRequest request = new SetMoodsicRequest(1L);
        Moodsic privateMoodsic = new Moodsic("Song", "/path", "audio/mpeg", member, false);
        privateMoodsic.setId(1L);

        when(userService.getCurrentUser()).thenReturn(owner);
        when(roomRepository.findByJoinCode("TESTCODE")).thenReturn(Optional.of(room));
        when(moodsicRepository.findById(1L)).thenReturn(Optional.of(privateMoodsic));

        assertThatThrownBy(() -> roomService.setMoodsic("TESTCODE", request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("don't have access");
    }
}
