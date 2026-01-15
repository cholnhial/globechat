package dev.chol.globechat.controller;

import dev.chol.globechat.dto.*;
import dev.chol.globechat.entity.MemberRole;
import dev.chol.globechat.service.ChatService;
import dev.chol.globechat.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for chat room endpoints.
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<RoomDto> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        RoomDto room = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(room);
    }

    @GetMapping("/{joinCode}")
    public ResponseEntity<RoomDto> getRoom(@PathVariable String joinCode) {
        RoomDto room = roomService.getRoom(joinCode);
        return ResponseEntity.ok(room);
    }

    @PutMapping("/{joinCode}")
    public ResponseEntity<RoomDto> updateRoom(
            @PathVariable String joinCode,
            @Valid @RequestBody UpdateRoomRequest request) {
        RoomDto room = roomService.updateRoom(joinCode, request);
        return ResponseEntity.ok(room);
    }

    @DeleteMapping("/{joinCode}")
    public ResponseEntity<Void> deleteRoom(@PathVariable String joinCode) {
        roomService.deleteRoom(joinCode);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{joinCode}/join")
    public ResponseEntity<RoomDto> joinRoom(@PathVariable String joinCode) {
        RoomDto room = roomService.joinRoom(joinCode);
        return ResponseEntity.ok(room);
    }

    @PostMapping("/{joinCode}/leave")
    public ResponseEntity<Void> leaveRoom(@PathVariable String joinCode) {
        roomService.leaveRoom(joinCode);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{joinCode}/members")
    public ResponseEntity<List<RoomMemberDto>> getMembers(@PathVariable String joinCode) {
        List<RoomMemberDto> members = roomService.getMembers(joinCode);
        return ResponseEntity.ok(members);
    }

    @PostMapping("/{joinCode}/kick/{username}")
    public ResponseEntity<Void> kickUser(
            @PathVariable String joinCode,
            @PathVariable String username) {
        roomService.kickUser(joinCode, username);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{joinCode}/ban")
    public ResponseEntity<Void> banUser(
            @PathVariable String joinCode,
            @Valid @RequestBody BanRequest request) {
        roomService.banUser(joinCode, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{joinCode}/ban/{username}")
    public ResponseEntity<Void> unbanUser(
            @PathVariable String joinCode,
            @PathVariable String username) {
        roomService.unbanUser(joinCode, username);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{joinCode}/mods/{username}")
    public ResponseEntity<Void> promoteMod(
            @PathVariable String joinCode,
            @PathVariable String username) {
        roomService.promoteMod(joinCode, username);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{joinCode}/mods/{username}")
    public ResponseEntity<Void> demoteMod(
            @PathVariable String joinCode,
            @PathVariable String username) {
        roomService.demoteMod(joinCode, username);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{joinCode}/moodsic")
    public ResponseEntity<RoomDto> setMoodsic(
            @PathVariable String joinCode,
            @Valid @RequestBody SetMoodsicRequest request) {
        RoomDto room = roomService.setMoodsic(joinCode, request);
        return ResponseEntity.ok(room);
    }

    @DeleteMapping("/{joinCode}/moodsic")
    public ResponseEntity<RoomDto> clearMoodsic(@PathVariable String joinCode) {
        RoomDto room = roomService.clearMoodsic(joinCode);
        return ResponseEntity.ok(room);
    }

    @PostMapping("/{joinCode}/moodsic/toggle")
    public ResponseEntity<RoomDto> toggleMoodsicPause(@PathVariable String joinCode) {
        RoomDto room = roomService.toggleMoodsicPause(joinCode);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/my")
    public ResponseEntity<List<RoomDto>> getMyRooms() {
        List<RoomDto> rooms = roomService.getMyRooms();
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/markers")
    public ResponseEntity<List<RoomMarkerDto>> getAllRoomMarkers() {
        List<RoomMarkerDto> markers = roomService.getAllRoomMarkers();
        return ResponseEntity.ok(markers);
    }

    @GetMapping("/{joinCode}/role")
    public ResponseEntity<MemberRole> getUserRole(@PathVariable String joinCode) {
        MemberRole role = roomService.getUserRole(joinCode);
        return ResponseEntity.ok(role);
    }

    @GetMapping("/{joinCode}/messages")
    public ResponseEntity<List<ChatMessageDto>> getMessages(
            @PathVariable String joinCode,
            @RequestParam(defaultValue = "100") int limit) {
        List<ChatMessageDto> messages = chatService.getRecentMessages(joinCode, limit);
        return ResponseEntity.ok(messages);
    }
}
