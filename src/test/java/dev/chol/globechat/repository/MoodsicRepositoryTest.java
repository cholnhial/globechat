package dev.chol.globechat.repository;

import dev.chol.globechat.TestcontainersConfiguration;
import dev.chol.globechat.entity.Moodsic;
import dev.chol.globechat.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class MoodsicRepositoryTest {

    @Autowired
    private MoodsicRepository moodsicRepository;

    @Autowired
    private UserRepository userRepository;

    private User uploader;
    private Moodsic publicMoodsic;
    private Moodsic privateMoodsic;

    @BeforeEach
    void setUp() {
        moodsicRepository.deleteAll();
        userRepository.deleteAll();

        uploader = new User("uploader", "uploader@example.com", "password");
        uploader = userRepository.save(uploader);

        publicMoodsic = new Moodsic("Public Song", "/path/public.mp3", "audio/mpeg", uploader, true);
        publicMoodsic.setPlayCount(10L);
        publicMoodsic = moodsicRepository.save(publicMoodsic);

        privateMoodsic = new Moodsic("Private Song", "/path/private.mp3", "audio/mpeg", uploader, false);
        privateMoodsic.setPlayCount(5L);
        privateMoodsic = moodsicRepository.save(privateMoodsic);
    }

    @Test
    void findByUploadedBy_returnsUserMoodsics() {
        List<Moodsic> moodsics = moodsicRepository.findByUploadedBy(uploader);

        assertThat(moodsics).hasSize(2);
    }

    @Test
    void findAllByOrderByPlayCountDesc_ordersCorrectly() {
        List<Moodsic> moodsics = moodsicRepository.findAllByOrderByPlayCountDesc();

        assertThat(moodsics).hasSize(2);
        assertThat(moodsics.get(0).getPlayCount()).isGreaterThanOrEqualTo(moodsics.get(1).getPlayCount());
    }

    @Test
    void searchByName_findsMatchingMoodsics() {
        List<Moodsic> moodsics = moodsicRepository.searchByName("public");

        assertThat(moodsics).hasSize(1);
        assertThat(moodsics.get(0).getName()).isEqualTo("Public Song");
    }

    @Test
    void findByIsPublicTrue_returnsOnlyPublicMoodsics() {
        List<Moodsic> moodsics = moodsicRepository.findByIsPublicTrue();

        assertThat(moodsics).hasSize(1);
        assertThat(moodsics.get(0).isPublic()).isTrue();
    }

    @Test
    void findByIsPublicTrueOrderByPlayCountDesc_ordersPublicMoodsics() {
        Moodsic anotherPublic = new Moodsic("Another Public", "/path/another.mp3", "audio/mpeg", uploader, true);
        anotherPublic.setPlayCount(20L);
        moodsicRepository.save(anotherPublic);

        List<Moodsic> moodsics = moodsicRepository.findByIsPublicTrueOrderByPlayCountDesc();

        assertThat(moodsics).hasSize(2);
        assertThat(moodsics.get(0).getPlayCount()).isEqualTo(20L);
    }

    @Test
    void findAvailableForUser_returnsPublicAndOwnedMoodsics() {
        User otherUser = new User("other", "other@example.com", "password");
        otherUser = userRepository.save(otherUser);

        Moodsic otherPrivate = new Moodsic("Other Private", "/path/other.mp3", "audio/mpeg", otherUser, false);
        moodsicRepository.save(otherPrivate);

        List<Moodsic> availableForUploader = moodsicRepository.findAvailableForUser(uploader);
        List<Moodsic> availableForOther = moodsicRepository.findAvailableForUser(otherUser);

        // Uploader can see: public + own private
        assertThat(availableForUploader).hasSize(2);
        // Other user can see: public + own private
        assertThat(availableForOther).hasSize(2);
    }

    @Test
    void incrementPlayCount_incrementsCount() {
        Long originalCount = publicMoodsic.getPlayCount();

        moodsicRepository.incrementPlayCount(publicMoodsic.getId());
        moodsicRepository.flush();

        Moodsic updated = moodsicRepository.findById(publicMoodsic.getId()).orElseThrow();
        assertThat(updated.getPlayCount()).isEqualTo(originalCount + 1);
    }

    @Test
    void save_setsCreatedAtAutomatically() {
        Moodsic newMoodsic = new Moodsic("New Song", "/path/new.mp3", "audio/mpeg", uploader, false);
        Moodsic saved = moodsicRepository.save(newMoodsic);

        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void save_setsDefaultPlayCount() {
        Moodsic newMoodsic = new Moodsic("New Song", "/path/new.mp3", "audio/mpeg", uploader, false);
        Moodsic saved = moodsicRepository.save(newMoodsic);

        assertThat(saved.getPlayCount()).isEqualTo(0L);
    }
}
