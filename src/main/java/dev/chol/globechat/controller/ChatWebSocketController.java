package dev.chol.globechat.controller;

import dev.chol.globechat.dto.ChatMessageDto;
import dev.chol.globechat.dto.SendMessageRequest;
import dev.chol.globechat.entity.MessageType;
import dev.chol.globechat.entity.User;
import dev.chol.globechat.service.ChatService;
import dev.chol.globechat.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket controller for real-time chat functionality.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle sending a chat message.
     */
    @MessageMapping("/chat/{joinCode}/send")
    @SendTo("/topic/room/{joinCode}")
    public ChatMessageDto sendMessage(
            @DestinationVariable String joinCode,
            @Payload SendMessageRequest request,
            Principal principal) {
        
        String username = principal.getName();
        log.debug("Message received for room {} from user {}: {}", joinCode, username, request.content());
        return chatService.sendMessage(joinCode, request.content(), username);
    }

    /**
     * Handle user joining a room's chat.
     */
    @MessageMapping("/chat/{joinCode}/join")
    @SendTo("/topic/room/{joinCode}")
    public ChatMessageDto userJoined(
            @DestinationVariable String joinCode,
            Principal principal) {
        
        String username = principal.getName();
        User user = userService.findByUsername(username);
        String content = user.getUsername() + " has joined the chat";
        log.debug("User {} joined room {}", username, joinCode);
        
        return chatService.createSystemMessage(joinCode, user, content, MessageType.JOIN);
    }

    /**
     * Handle user leaving a room's chat.
     */
    @MessageMapping("/chat/{joinCode}/leave")
    @SendTo("/topic/room/{joinCode}")
    public ChatMessageDto userLeft(
            @DestinationVariable String joinCode,
            Principal principal) {
        
        String username = principal.getName();
        User user = userService.findByUsername(username);
        String content = user.getUsername() + " has left the chat";
        log.debug("User {} left room {}", username, joinCode);
        
        return chatService.createSystemMessage(joinCode, user, content, MessageType.LEAVE);
    }

    /**
     * Broadcast a message to a specific room (for use by services).
     */
    public void broadcastToRoom(String joinCode, ChatMessageDto message) {
        messagingTemplate.convertAndSend("/topic/room/" + joinCode, message);
    }

    /**
     * Broadcast kick notification to the room.
     */
    public void broadcastKick(String joinCode, String kickedUsername, String kickedByUsername) {
        String content = kickedUsername + " was kicked by " + kickedByUsername;
        ChatMessageDto message = ChatMessageDto.kickMessage(
                content,
                MessageType.KICK,
                joinCode,
                kickedUsername
        );
        messagingTemplate.convertAndSend("/topic/room/" + joinCode, message);
    }

    /**
     * Broadcast ban notification to the room.
     */
    public void broadcastBan(String joinCode, String bannedUsername, String bannedByUsername, String reason) {
        String content = bannedUsername + " was banned by " + bannedByUsername;
        if (reason != null && !reason.isBlank()) {
            content += " (Reason: " + reason + ")";
        }
        ChatMessageDto message = ChatMessageDto.kickMessage(
                content,
                MessageType.BAN,
                joinCode,
                bannedUsername
        );
        messagingTemplate.convertAndSend("/topic/room/" + joinCode, message);
    }

    /**
     * Broadcast room destroyed event.
     */
    public void broadcastRoomDestroyed(String joinCode) {
        ChatMessageDto message = ChatMessageDto.systemMessage(
                "This room has been destroyed by the owner",
                MessageType.ROOM_DESTROYED,
                joinCode
        );
        messagingTemplate.convertAndSend("/topic/room/" + joinCode, message);
    }
}
