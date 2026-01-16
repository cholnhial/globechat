package dev.chol.globechat.service;

import dev.chol.globechat.dto.MoodsicDto;
import dev.chol.globechat.entity.ChatRoom;
import dev.chol.globechat.entity.Moodsic;
import dev.chol.globechat.entity.User;
import dev.chol.globechat.exception.BadRequestException;
import dev.chol.globechat.exception.ForbiddenException;
import dev.chol.globechat.exception.ResourceNotFoundException;
import dev.chol.globechat.repository.ChatRoomMemberRepository;
import dev.chol.globechat.repository.ChatRoomRepository;
import dev.chol.globechat.repository.MoodsicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Service for moodsic operations.
 */
@Service
@RequiredArgsConstructor
public class MoodsicService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "audio/mpeg",
            "audio/mp3",
            "audio/wav",
            "audio/ogg",
            "audio/webm"
    );

    private final MoodsicRepository moodsicRepository;
    private final MoodsicStorageService storageService;
    private final UserService userService;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    /**
     * Upload a new moodsic.
     */
    @Transactional
    public MoodsicDto upload(MultipartFile file, String name, boolean isPublic) {
        User currentUser = userService.getCurrentUser();

        // Validate file
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Invalid file type. Allowed types: MP3, WAV, OGG, WebM");
        }

        try {
            String filePath = storageService.store(file);

            Moodsic moodsic = new Moodsic(name, filePath, contentType, currentUser, isPublic);
            moodsic = moodsicRepository.save(moodsic);

            return MoodsicDto.from(moodsic);
        } catch (IOException e) {
            throw new BadRequestException("Failed to store file: " + e.getMessage());
        }
    }

    /**
     * Get a moodsic by ID.
     */
    @Transactional(readOnly = true)
    public MoodsicDto getById(Long id) {
        User currentUser = userService.getCurrentUser();
        Moodsic moodsic = findById(id);

        // Check access
        if (!moodsic.isPublic() && !moodsic.getUploadedBy().equals(currentUser)) {
            throw new ForbiddenException("You don't have access to this moodsic");
        }

        return MoodsicDto.from(moodsic);
    }

    /**
     * List all moodsics available to the current user.
     */
    @Transactional(readOnly = true)
    public List<MoodsicDto> listAvailable() {
        User currentUser = userService.getCurrentUser();
        return moodsicRepository.findAvailableForUser(currentUser).stream()
                .map(MoodsicDto::from)
                .toList();
    }

    /**
     * List moodsics uploaded by the current user.
     */
    @Transactional(readOnly = true)
    public List<MoodsicDto> listMyMoodsics() {
        User currentUser = userService.getCurrentUser();
        return moodsicRepository.findByUploadedBy(currentUser).stream()
                .map(MoodsicDto::from)
                .toList();
    }

    /**
     * List all public moodsics.
     */
    @Transactional(readOnly = true)
    public List<MoodsicDto> listPublic() {
        return moodsicRepository.findByIsPublicTrueOrderByPlayCountDesc().stream()
                .map(MoodsicDto::from)
                .toList();
    }

    /**
     * Toggle moodsic visibility (uploader only).
     */
    @Transactional
    public MoodsicDto toggleVisibility(Long id) {
        User currentUser = userService.getCurrentUser();
        Moodsic moodsic = findById(id);

        if (!moodsic.getUploadedBy().equals(currentUser)) {
            throw new ForbiddenException("Only the uploader can change visibility");
        }

        moodsic.setPublic(!moodsic.isPublic());
        moodsic = moodsicRepository.save(moodsic);

        return MoodsicDto.from(moodsic);
    }

    /**
     * Delete a moodsic (uploader only).
     */
    @Transactional
    public void delete(Long id) {
        User currentUser = userService.getCurrentUser();
        Moodsic moodsic = findById(id);

        if (!moodsic.getUploadedBy().equals(currentUser)) {
            throw new ForbiddenException("Only the uploader can delete this moodsic");
        }

        // Delete file from storage
        try {
            storageService.delete(moodsic.getFilePath());
        } catch (IOException e) {
            // Log but don't fail the deletion
        }

        moodsicRepository.delete(moodsic);
    }

    /**
     * Search moodsics by name with filter.
     */
    @Transactional(readOnly = true)
    public List<MoodsicDto> search(String query, String filter) {
        User currentUser = userService.getCurrentUser();
        List<Moodsic> moodsics;

        if ("mine".equalsIgnoreCase(filter)) {
            moodsics = moodsicRepository.searchByNameAndUser(query, currentUser);
        } else {
            moodsics = moodsicRepository.searchAvailableForUser(query, currentUser);
        }

        return moodsics.stream()
                .map(MoodsicDto::from)
                .toList();
    }

    /**
     * Get file path for streaming a moodsic.
     * Access is allowed if:
     * 1. The moodsic is public, OR
     * 2. The user is the uploader, OR
     * 3. The user is a member of a room where this moodsic is currently playing
     */
    @Transactional(readOnly = true)
    public MoodsicFileInfo getFileInfo(Long id) {
        Moodsic moodsic = findById(id);

        // Public moodsics can be streamed by anyone
        if (moodsic.isPublic()) {
            return new MoodsicFileInfo(
                    storageService.getFilePath(moodsic.getFilePath()),
                    moodsic.getContentType()
            );
        }

        // For private moodsics, check user authentication and access rights
        User currentUser = userService.getCurrentUserOrNull();
        if (currentUser == null) {
            throw new ForbiddenException("You don't have access to this moodsic");
        }

        // Allow access if user is the uploader
        if (moodsic.getUploadedBy().equals(currentUser)) {
            return new MoodsicFileInfo(
                    storageService.getFilePath(moodsic.getFilePath()),
                    moodsic.getContentType()
            );
        }

        // Allow access if user is a member of any room where this moodsic is currently playing
        List<ChatRoom> roomsPlayingMoodsic = chatRoomRepository.findByCurrentMoodsicId(id);
        for (ChatRoom room : roomsPlayingMoodsic) {
            if (chatRoomMemberRepository.existsByUserAndChatRoom(currentUser, room)) {
                return new MoodsicFileInfo(
                        storageService.getFilePath(moodsic.getFilePath()),
                        moodsic.getContentType()
                );
            }
        }

        throw new ForbiddenException("You don't have access to this moodsic");
    }

    /**
     * Record for returning file info for streaming.
     */
    public record MoodsicFileInfo(java.nio.file.Path path, String contentType) {}

    private Moodsic findById(Long id) {
        return moodsicRepository.findByIdWithAssociations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Moodsic", "id", id));
    }
}
