package dev.chol.globechat.repository;

import dev.chol.globechat.TestcontainersConfiguration;
import dev.chol.globechat.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ChatRoomMemberRepositoryTest {

    @Autowired
    private ChatRoomMemberRepository memberRepository;

    @Autowired
    private ChatRoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private User member;
    private ChatRoom room;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
        roomRepository.deleteAll();
        userRepository.deleteAll();

        owner = new User("owner", "owner@example.com", "password");
        owner = userRepository.save(owner);

        member = new User("member", "member@example.com", "password");
        member = userRepository.save(member);

        room = new ChatRoom("Test Room", "Description", owner, 40.7128, -74.0060);
        room = roomRepository.save(room);

        ChatRoomMember ownerMember = new ChatRoomMember(owner, room, MemberRole.OWNER);
        memberRepository.save(ownerMember);

        ChatRoomMember regularMember = new ChatRoomMember(member, room, MemberRole.CHATTER);
        memberRepository.save(regularMember);
    }

    @Test
    void findByUser_returnsUserMemberships() {
        List<ChatRoomMember> memberships = memberRepository.findByUser(member);

        assertThat(memberships).hasSize(1);
        assertThat(memberships.get(0).getRole()).isEqualTo(MemberRole.CHATTER);
    }

    @Test
    void findByChatRoom_returnsRoomMembers() {
        List<ChatRoomMember> members = memberRepository.findByChatRoom(room);

        assertThat(members).hasSize(2);
    }

    @Test
    void findByUserAndChatRoom_whenExists_returnsMembership() {
        Optional<ChatRoomMember> membership = memberRepository.findByUserAndChatRoom(member, room);

        assertThat(membership).isPresent();
        assertThat(membership.get().getRole()).isEqualTo(MemberRole.CHATTER);
    }

    @Test
    void findByUserAndChatRoom_whenNotExists_returnsEmpty() {
        User nonMember = new User("nonmember", "nonmember@example.com", "password");
        nonMember = userRepository.save(nonMember);

        Optional<ChatRoomMember> membership = memberRepository.findByUserAndChatRoom(nonMember, room);

        assertThat(membership).isEmpty();
    }

    @Test
    void findByChatRoomAndRole_returnsMatchingMembers() {
        List<ChatRoomMember> owners = memberRepository.findByChatRoomAndRole(room, MemberRole.OWNER);

        assertThat(owners).hasSize(1);
        assertThat(owners.get(0).getUser()).isEqualTo(owner);
    }

    @Test
    void existsByUserAndChatRoom_whenExists_returnsTrue() {
        boolean exists = memberRepository.existsByUserAndChatRoom(member, room);

        assertThat(exists).isTrue();
    }

    @Test
    void existsByUserAndChatRoom_whenNotExists_returnsFalse() {
        User nonMember = new User("nonmember", "nonmember@example.com", "password");
        nonMember = userRepository.save(nonMember);

        boolean exists = memberRepository.existsByUserAndChatRoom(nonMember, room);

        assertThat(exists).isFalse();
    }

    @Test
    void findRoomsByUser_returnsJoinedRooms() {
        List<ChatRoom> rooms = memberRepository.findRoomsByUser(member);

        assertThat(rooms).hasSize(1);
        assertThat(rooms.get(0)).isEqualTo(room);
    }

    @Test
    void countByChatRoom_returnsCorrectCount() {
        long count = memberRepository.countByChatRoom(room);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void countModsByChatRoom_countsOnlyMods() {
        User mod = new User("mod", "mod@example.com", "password");
        mod = userRepository.save(mod);
        ChatRoomMember modMember = new ChatRoomMember(mod, room, MemberRole.MOD);
        memberRepository.save(modMember);

        long modCount = memberRepository.countModsByChatRoom(room);

        assertThat(modCount).isEqualTo(1);
    }
}
