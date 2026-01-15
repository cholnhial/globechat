package dev.chol.globechat.repository;

import dev.chol.globechat.TestcontainersConfiguration;
import dev.chol.globechat.entity.ChatRoom;
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
class ChatRoomRepositoryTest {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private ChatRoom testRoom;

    @BeforeEach
    void setUp() {
        chatRoomRepository.deleteAll();
        userRepository.deleteAll();

        owner = new User("owner", "owner@example.com", "password");
        owner = userRepository.save(owner);

        testRoom = new ChatRoom("Test Room", "A test room", owner, 40.7128, -74.0060);
        testRoom = chatRoomRepository.save(testRoom);
    }

    @Test
    void findByJoinCode_whenRoomExists_returnsRoom() {
        Optional<ChatRoom> found = chatRoomRepository.findByJoinCode(testRoom.getJoinCode());

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Test Room");
    }

    @Test
    void findByJoinCode_whenRoomDoesNotExist_returnsEmpty() {
        Optional<ChatRoom> found = chatRoomRepository.findByJoinCode("NOTEXIST");

        assertThat(found).isEmpty();
    }

    @Test
    void findByOwner_returnsOwnedRooms() {
        ChatRoom anotherRoom = new ChatRoom("Another Room", "Description", owner, 51.5074, -0.1278);
        chatRoomRepository.save(anotherRoom);

        User otherOwner = new User("other", "other@example.com", "password");
        otherOwner = userRepository.save(otherOwner);
        ChatRoom otherRoom = new ChatRoom("Other Room", "Description", otherOwner, 48.8566, 2.3522);
        chatRoomRepository.save(otherRoom);

        List<ChatRoom> rooms = chatRoomRepository.findByOwner(owner);

        assertThat(rooms).hasSize(2);
        assertThat(rooms).allMatch(r -> r.getOwner().equals(owner));
    }

    @Test
    void existsByJoinCode_whenExists_returnsTrue() {
        boolean exists = chatRoomRepository.existsByJoinCode(testRoom.getJoinCode());

        assertThat(exists).isTrue();
    }

    @Test
    void existsByJoinCode_whenNotExists_returnsFalse() {
        boolean exists = chatRoomRepository.existsByJoinCode("NOTEXIST");

        assertThat(exists).isFalse();
    }

    @Test
    void searchByTitle_findsByPartialMatch() {
        ChatRoom anotherRoom = new ChatRoom("Gaming Room", "Description", owner, 35.6762, 139.6503);
        chatRoomRepository.save(anotherRoom);

        List<ChatRoom> rooms = chatRoomRepository.searchByTitle("test");

        assertThat(rooms).hasSize(1);
        assertThat(rooms.get(0).getTitle()).isEqualTo("Test Room");
    }

    @Test
    void save_generatesJoinCodeAutomatically() {
        ChatRoom newRoom = new ChatRoom("New Room", "Description", owner, 34.0522, -118.2437);
        ChatRoom saved = chatRoomRepository.save(newRoom);

        assertThat(saved.getJoinCode()).isNotNull();
        assertThat(saved.getJoinCode()).hasSize(8);
    }

    @Test
    void save_setsCreatedAtAutomatically() {
        ChatRoom newRoom = new ChatRoom("New Room", "Description", owner, 37.7749, -122.4194);
        ChatRoom saved = chatRoomRepository.save(newRoom);

        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
