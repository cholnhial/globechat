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
 * Represents a mood music (moodsic) that can be played in chat rooms.
 * Moodsics are stored on disk; this entity stores the file path reference.
 * A moodsic can be shared across multiple rooms and tracks play count for popularity.
 */
@Entity
@Table(name = "moodsics")
@Getter
@Setter
@NoArgsConstructor
public class Moodsic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Display name for the moodsic.
     */
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * Path to the audio file on disk, relative to the configured storage directory.
     */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    /**
     * MIME content type of the audio file (e.g., "audio/mpeg" for MP3).
     */
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    /**
     * User who uploaded this moodsic.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;

    /**
     * Number of times this moodsic has been set as the active moodsic in a room.
     * Used to track popularity.
     */
    @Column(name = "play_count", nullable = false)
    private Long playCount = 0L;

    /**
     * Whether this moodsic is publicly visible to all users.
     * Private moodsics are only visible to the uploader.
     */
    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Rooms currently playing this moodsic.
     */
    @OneToMany(mappedBy = "currentMoodsic", fetch = FetchType.LAZY)
    private Set<ChatRoom> activeInRooms = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.playCount == null) {
            this.playCount = 0L;
        }
    }

    public Moodsic(String name, String filePath, String contentType, User uploadedBy, boolean isPublic) {
        this.name = name;
        this.filePath = filePath;
        this.contentType = contentType;
        this.uploadedBy = uploadedBy;
        this.isPublic = isPublic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Moodsic moodsic = (Moodsic) o;
        return id != null && id.equals(moodsic.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Moodsic{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", filePath='" + filePath + '\'' +
                ", playCount=" + playCount +
                '}';
    }
}
