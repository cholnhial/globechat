package dev.chol.globechat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a chat room on the globe.
 * Each room has a unique join code (alphanumeric) that can be shared via QR code.
 * Business identifier: joinCode (unique, used for equals/hashCode).
 */
@Entity
@Table(name = "chat_rooms")
@Getter
@Setter
@NoArgsConstructor
public class ChatRoom {

    private static final String ALPHANUMERIC = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int JOIN_CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique alphanumeric code for joining the room.
     * Can be used to generate QR codes.
     */
    @Column(name = "join_code", nullable = false, unique = true, length = 20)
    private String joinCode;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    /**
     * Room rules in dot-point format (text, can contain newlines).
     */
    @Column(columnDefinition = "TEXT")
    private String rules;

    /**
     * Owner of the room - has full control.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /**
     * Currently playing moodsic for the room (nullable - room may have no music).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_moodsic_id")
    private Moodsic currentMoodsic;

    /**
     * Whether the moodsic is currently paused.
     */
    @Column(name = "moodsic_paused", nullable = false)
    private boolean moodsicPaused = false;

    /**
     * Latitude coordinate for the room's position on the globe.
     */
    @Column(nullable = false)
    private Double latitude = 0.0;

    /**
     * Longitude coordinate for the room's position on the globe.
     */
    @Column(nullable = false)
    private Double longitude = 0.0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Members of this room (including owner and mods).
     */
    @OneToMany(mappedBy = "chatRoom", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ChatRoomMember> members = new HashSet<>();

    /**
     * Users banned from this room.
     */
    @OneToMany(mappedBy = "chatRoom", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RoomBan> bans = new HashSet<>();

    /**
     * Messages in this room.
     */
    @OneToMany(mappedBy = "chatRoom", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ChatMessage> messages = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.joinCode == null) {
            this.joinCode = generateJoinCode();
        }
    }

    /**
     * Generates a random alphanumeric join code.
     * Excludes ambiguous characters (0, O, 1, I) for readability.
     */
    private static String generateJoinCode() {
        StringBuilder sb = new StringBuilder(JOIN_CODE_LENGTH);
        for (int i = 0; i < JOIN_CODE_LENGTH; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    public ChatRoom(String title, String description, User owner, Double latitude, Double longitude) {
        this.title = title;
        this.description = description;
        this.owner = owner;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatRoom chatRoom = (ChatRoom) o;
        return joinCode != null && joinCode.equals(chatRoom.joinCode);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(joinCode);
    }

    @Override
    public String toString() {
        return "ChatRoom{" +
                "id=" + id +
                ", joinCode='" + joinCode + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
