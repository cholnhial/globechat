package dev.chol.globechat.repository;

import dev.chol.globechat.TestcontainersConfiguration;
import dev.chol.globechat.entity.ChatRoom;
import dev.chol.globechat.entity.RoomBan;
import dev.chol.globechat.entity.User;
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
class RoomBanRepositoryTest {

    @Autowired
    private RoomBanRepository banRepository;

    @Autowired
    private ChatRoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private User bannedUser;
    private ChatRoom room;
    private RoomBan ban;

    @BeforeEach
    void setUp() {
        banRepository.deleteAll();
        roomRepository.deleteAll();
        userRepository.deleteAll();

        owner = new User("owner", "owner@example.com", "password");
        owner = userRepository.save(owner);

        bannedUser = new User("banned", "banned@example.com", "password");
        bannedUser = userRepository.save(bannedUser);

        room = new ChatRoom("Test Room", "Description", owner, 40.7128, -74.0060);
        room = roomRepository.save(room);

        ban = new RoomBan(room, bannedUser, owner, "Breaking rules");
        ban = banRepository.save(ban);
    }

    @Test
    void findByChatRoomAndBannedUser_whenExists_returnsBan() {
        Optional<RoomBan> found = banRepository.findByChatRoomAndBannedUser(room, bannedUser);

        assertThat(found).isPresent();
        assertThat(found.get().getReason()).isEqualTo("Breaking rules");
    }

    @Test
    void findByChatRoomAndBannedUser_whenNotExists_returnsEmpty() {
        User notBanned = new User("notbanned", "notbanned@example.com", "password");
        notBanned = userRepository.save(notBanned);

        Optional<RoomBan> found = banRepository.findByChatRoomAndBannedUser(room, notBanned);

        assertThat(found).isEmpty();
    }

    @Test
    void existsByChatRoomAndBannedUser_whenExists_returnsTrue() {
        boolean exists = banRepository.existsByChatRoomAndBannedUser(room, bannedUser);

        assertThat(exists).isTrue();
    }

    @Test
    void existsByChatRoomAndBannedUser_whenNotExists_returnsFalse() {
        User notBanned = new User("notbanned", "notbanned@example.com", "password");
        notBanned = userRepository.save(notBanned);

        boolean exists = banRepository.existsByChatRoomAndBannedUser(room, notBanned);

        assertThat(exists).isFalse();
    }

    @Test
    void findByChatRoom_returnsBansInRoom() {
        List<RoomBan> bans = banRepository.findByChatRoom(room);

        assertThat(bans).hasSize(1);
    }

    @Test
    void findByBannedUser_returnsUserBans() {
        List<RoomBan> bans = banRepository.findByBannedUser(bannedUser);

        assertThat(bans).hasSize(1);
    }

    @Test
    void findByBannedBy_returnsBansIssuedByUser() {
        List<RoomBan> bans = banRepository.findByBannedBy(owner);

        assertThat(bans).hasSize(1);
    }

    @Test
    void deleteByChatRoomAndBannedUser_removesBan() {
        banRepository.deleteByChatRoomAndBannedUser(room, bannedUser);
        banRepository.flush();

        boolean exists = banRepository.existsByChatRoomAndBannedUser(room, bannedUser);

        assertThat(exists).isFalse();
    }

    @Test
    void save_setsBannedAtAutomatically() {
        User anotherUser = new User("another", "another@example.com", "password");
        anotherUser = userRepository.save(anotherUser);

        RoomBan newBan = new RoomBan(room, anotherUser, owner, "Another reason");
        RoomBan saved = banRepository.save(newBan);

        assertThat(saved.getBannedAt()).isNotNull();
    }
}
