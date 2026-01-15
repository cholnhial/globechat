package dev.chol.globechat.service;

import dev.chol.globechat.dto.MoodsicDto;
import dev.chol.globechat.entity.Moodsic;
import dev.chol.globechat.entity.User;
import dev.chol.globechat.exception.BadRequestException;
import dev.chol.globechat.exception.ForbiddenException;
import dev.chol.globechat.exception.ResourceNotFoundException;
import dev.chol.globechat.repository.MoodsicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MoodsicServiceTest {

    @Mock
    private MoodsicRepository moodsicRepository;

    @Mock
    private MoodsicStorageService storageService;

    @Mock
    private UserService userService;

    @InjectMocks
    private MoodsicService moodsicService;

    private User uploader;
    private User otherUser;
    private Moodsic publicMoodsic;
    private Moodsic privateMoodsic;

    @BeforeEach
    void setUp() {
        uploader = new User("uploader", "uploader@example.com", "password");
        uploader.setId(1L);

        otherUser = new User("other", "other@example.com", "password");
        otherUser.setId(2L);

        publicMoodsic = new Moodsic("Public Song", "/path/public.mp3", "audio/mpeg", uploader, true);
        publicMoodsic.setId(1L);

        privateMoodsic = new Moodsic("Private Song", "/path/private.mp3", "audio/mpeg", uploader, false);
        privateMoodsic.setId(2L);
    }

    @Test
    void upload_withValidFile_savesMoodsic() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "song.mp3", "audio/mpeg", "audio content".getBytes());

        when(userService.getCurrentUser()).thenReturn(uploader);
        when(storageService.store(file)).thenReturn("stored-file.mp3");
        when(moodsicRepository.save(any(Moodsic.class))).thenReturn(publicMoodsic);

        MoodsicDto result = moodsicService.upload(file, "My Song", true);

        assertThat(result.name()).isEqualTo("Public Song");
        verify(storageService).store(file);
        verify(moodsicRepository).save(any(Moodsic.class));
    }

    @Test
    void upload_withEmptyFile_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "song.mp3", "audio/mpeg", new byte[0]);

        when(userService.getCurrentUser()).thenReturn(uploader);

        assertThatThrownBy(() -> moodsicService.upload(file, "My Song", true))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void upload_withInvalidContentType_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "song.txt", "text/plain", "not audio".getBytes());

        when(userService.getCurrentUser()).thenReturn(uploader);

        assertThatThrownBy(() -> moodsicService.upload(file, "My Song", true))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid file type");
    }

    @Test
    void getById_publicMoodsic_returnsForAnyUser() {
        when(userService.getCurrentUser()).thenReturn(otherUser);
        when(moodsicRepository.findByIdWithAssociations(1L)).thenReturn(Optional.of(publicMoodsic));

        MoodsicDto result = moodsicService.getById(1L);

        assertThat(result.name()).isEqualTo("Public Song");
    }

    @Test
    void getById_privateMoodsic_returnsForOwner() {
        when(userService.getCurrentUser()).thenReturn(uploader);
        when(moodsicRepository.findByIdWithAssociations(2L)).thenReturn(Optional.of(privateMoodsic));

        MoodsicDto result = moodsicService.getById(2L);

        assertThat(result.name()).isEqualTo("Private Song");
    }

    @Test
    void getById_privateMoodsic_throwsForNonOwner() {
        when(userService.getCurrentUser()).thenReturn(otherUser);
        when(moodsicRepository.findByIdWithAssociations(2L)).thenReturn(Optional.of(privateMoodsic));

        assertThatThrownBy(() -> moodsicService.getById(2L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("don't have access");
    }

    @Test
    void getById_whenNotExists_throwsException() {
        when(userService.getCurrentUser()).thenReturn(uploader);
        when(moodsicRepository.findByIdWithAssociations(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> moodsicService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listAvailable_returnsAvailableMoodsics() {
        when(userService.getCurrentUser()).thenReturn(uploader);
        when(moodsicRepository.findAvailableForUser(uploader))
                .thenReturn(List.of(publicMoodsic, privateMoodsic));

        List<MoodsicDto> result = moodsicService.listAvailable();

        assertThat(result).hasSize(2);
    }

    @Test
    void listPublic_returnsOnlyPublicMoodsics() {
        when(moodsicRepository.findByIsPublicTrueOrderByPlayCountDesc())
                .thenReturn(List.of(publicMoodsic));

        List<MoodsicDto> result = moodsicService.listPublic();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isPublic()).isTrue();
    }

    @Test
    void toggleVisibility_byOwner_togglesVisibility() {
        when(userService.getCurrentUser()).thenReturn(uploader);
        when(moodsicRepository.findByIdWithAssociations(1L)).thenReturn(Optional.of(publicMoodsic));
        when(moodsicRepository.save(any(Moodsic.class))).thenAnswer(inv -> inv.getArgument(0));

        MoodsicDto result = moodsicService.toggleVisibility(1L);

        assertThat(result.isPublic()).isFalse(); // Was true, now false
    }

    @Test
    void toggleVisibility_byNonOwner_throwsException() {
        when(userService.getCurrentUser()).thenReturn(otherUser);
        when(moodsicRepository.findByIdWithAssociations(1L)).thenReturn(Optional.of(publicMoodsic));

        assertThatThrownBy(() -> moodsicService.toggleVisibility(1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("uploader can change");
    }

    @Test
    void delete_byOwner_deletesMoodsic() throws IOException {
        when(userService.getCurrentUser()).thenReturn(uploader);
        when(moodsicRepository.findByIdWithAssociations(1L)).thenReturn(Optional.of(publicMoodsic));

        moodsicService.delete(1L);

        verify(storageService).delete(publicMoodsic.getFilePath());
        verify(moodsicRepository).delete(publicMoodsic);
    }

    @Test
    void delete_byNonOwner_throwsException() {
        when(userService.getCurrentUser()).thenReturn(otherUser);
        when(moodsicRepository.findByIdWithAssociations(1L)).thenReturn(Optional.of(publicMoodsic));

        assertThatThrownBy(() -> moodsicService.delete(1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("uploader can delete");
    }
}
