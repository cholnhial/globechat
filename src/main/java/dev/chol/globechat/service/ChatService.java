package dev.chol.globechat.service;

import dev.chol.globechat.dto.ChatMessageDto;
import dev.chol.globechat.entity.*;
import dev.chol.globechat.exception.ForbiddenException;
import dev.chol.globechat.exception.ResourceNotFoundException;
import dev.chol.globechat.repository.ChatMessageRepository;
import dev.chol.globechat.repository.ChatRoomMemberRepository;
import dev.chol.globechat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Service for chat message operations.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository messageRepository;
    private final ChatRoomRepository roomRepository;
    private final ChatRoomMemberRepository memberRepository;
    private final UserService userService;

    /**
     * Send a chat message to a room.
     */
    @Transactional
    public ChatMessageDto sendMessage(String joinCode, String content, String username) {
        User currentUser = userService.findByUsername(username);
        ChatRoom room = findRoomByJoinCode(joinCode);

        verifyMembership(room, currentUser);

        ChatMessage message = new ChatMessage(room, currentUser, content, MessageType.CHAT);
        message = messageRepository.save(message);

        return ChatMessageDto.from(message);
    }

    /**
     * Create a system message (for events like join/leave/kick).
     */
    @Transactional
    public ChatMessageDto createSystemMessage(String joinCode, User user, String content, MessageType type) {
        ChatRoom room = findRoomByJoinCode(joinCode);

        ChatMessage message = new ChatMessage(room, user, content, type);
        message = messageRepository.save(message);

        return ChatMessageDto.from(message);
    }

    /**
     * Get recent messages for a room.
     */
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getRecentMessages(String joinCode, int limit) {
        User currentUser = userService.getCurrentUser();
        ChatRoom room = findRoomByJoinCode(joinCode);

        verifyMembership(room, currentUser);

        List<ChatMessage> messages = messageRepository.findLastNMessages(room, PageRequest.of(0, limit));
        // Reverse to get chronological order
        Collections.reverse(messages);

        return messages.stream()
                .map(ChatMessageDto::from)
                .toList();
    }

    /**
     * Get all messages for a room (for initial load).
     */
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getAllMessages(String joinCode) {
        User currentUser = userService.getCurrentUser();
        ChatRoom room = findRoomByJoinCode(joinCode);

        verifyMembership(room, currentUser);

        return messageRepository.findByChatRoomOrderByCreatedAtAsc(room).stream()
                .map(ChatMessageDto::from)
                .toList();
    }

    private ChatRoom findRoomByJoinCode(String joinCode) {
        return roomRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "joinCode", joinCode));
    }

    private void verifyMembership(ChatRoom room, User user) {
        if (!memberRepository.existsByUserAndChatRoom(user, room)) {
            throw new ForbiddenException("You must be a member of this room");
        }
    }
}
