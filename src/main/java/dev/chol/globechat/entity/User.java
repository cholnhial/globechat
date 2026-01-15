package dev.chol.globechat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a user in the GlobeChat application.
 * Users can create rooms, join rooms, and upload moodsics.
 * Business identifier: username (unique, used for equals/hashCode).
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Rooms this user owns.
     */
    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
    private Set<ChatRoom> ownedRooms = new HashSet<>();

    /**
     * Room memberships for this user (includes all rooms they've joined).
     */
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<ChatRoomMember> memberships = new HashSet<>();

    /**
     * Moodsics uploaded by this user.
     */
    @OneToMany(mappedBy = "uploadedBy", fetch = FetchType.LAZY)
    private Set<Moodsic> uploadedMoodsics = new HashSet<>();

    /**
     * Bans issued against this user.
     */
    @OneToMany(mappedBy = "bannedUser", fetch = FetchType.LAZY)
    private Set<RoomBan> bansReceived = new HashSet<>();

    /**
     * Bans issued by this user.
     */
    @OneToMany(mappedBy = "bannedBy", fetch = FetchType.LAZY)
    private Set<RoomBan> bansIssued = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public User(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return username != null && username.equals(user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(username);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
