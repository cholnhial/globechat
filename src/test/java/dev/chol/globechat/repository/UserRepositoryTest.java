package dev.chol.globechat.repository;

import dev.chol.globechat.TestcontainersConfiguration;
import dev.chol.globechat.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        testUser = new User("testuser", "test@example.com", "hashedpassword");
        testUser = userRepository.save(testUser);
    }

    @Test
    void findByUsername_whenUserExists_returnsUser() {
        Optional<User> found = userRepository.findByUsername("testuser");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void findByUsername_whenUserDoesNotExist_returnsEmpty() {
        Optional<User> found = userRepository.findByUsername("nonexistent");

        assertThat(found).isEmpty();
    }

    @Test
    void findByEmail_whenUserExists_returnsUser() {
        Optional<User> found = userRepository.findByEmail("test@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    void findByEmail_whenUserDoesNotExist_returnsEmpty() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    void existsByUsername_whenUserExists_returnsTrue() {
        boolean exists = userRepository.existsByUsername("testuser");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByUsername_whenUserDoesNotExist_returnsFalse() {
        boolean exists = userRepository.existsByUsername("nonexistent");

        assertThat(exists).isFalse();
    }

    @Test
    void existsByEmail_whenUserExists_returnsTrue() {
        boolean exists = userRepository.existsByEmail("test@example.com");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_whenUserDoesNotExist_returnsFalse() {
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        assertThat(exists).isFalse();
    }

    @Test
    void save_setsCreatedAtAutomatically() {
        User newUser = new User("newuser", "new@example.com", "password");
        User saved = userRepository.save(newUser);

        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
