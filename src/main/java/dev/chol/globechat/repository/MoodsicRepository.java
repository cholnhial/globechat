package dev.chol.globechat.repository;

import dev.chol.globechat.entity.Moodsic;
import dev.chol.globechat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Moodsic entities.
 * Queries use JOIN FETCH for lazy associations to support GraalVM native image.
 */
@Repository
public interface MoodsicRepository extends JpaRepository<Moodsic, Long> {

    /**
     * Find all moodsics uploaded by a specific user.
     * Uses JOIN FETCH for lazy associations to support GraalVM native image.
     */
    @Query("SELECT m FROM Moodsic m JOIN FETCH m.uploadedBy WHERE m.uploadedBy = :uploadedBy")
    List<Moodsic> findByUploadedBy(@Param("uploadedBy") User uploadedBy);

    /**
     * Find all moodsics ordered by play count (most popular first).
     * Uses JOIN FETCH for lazy associations to support GraalVM native image.
     */
    @Query("SELECT m FROM Moodsic m JOIN FETCH m.uploadedBy ORDER BY m.playCount DESC")
    List<Moodsic> findAllByOrderByPlayCountDesc();

    /**
     * Find top N most popular moodsics.
     * Uses JOIN FETCH for lazy associations to support GraalVM native image.
     */
    @Query("SELECT m FROM Moodsic m JOIN FETCH m.uploadedBy ORDER BY m.playCount DESC LIMIT :n")
    List<Moodsic> findTopNByOrderByPlayCountDesc(@Param("n") int n);

    /**
     * Search moodsics by name (case-insensitive).
     * Uses JOIN FETCH for lazy associations to support GraalVM native image.
     */
    @Query("SELECT m FROM Moodsic m JOIN FETCH m.uploadedBy WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Moodsic> searchByName(@Param("query") String query);

    /**
     * Increment the play count for a moodsic.
     */
    @Modifying
    @Query("UPDATE Moodsic m SET m.playCount = m.playCount + 1 WHERE m.id = :id")
    void incrementPlayCount(@Param("id") Long id);

    /**
     * Find all public moodsics.
     * Uses JOIN FETCH for lazy associations to support GraalVM native image.
     */
    @Query("SELECT m FROM Moodsic m JOIN FETCH m.uploadedBy WHERE m.isPublic = true")
    List<Moodsic> findByIsPublicTrue();

    /**
     * Find all public moodsics ordered by play count.
     * Uses JOIN FETCH for lazy associations to support GraalVM native image.
     */
    @Query("SELECT m FROM Moodsic m JOIN FETCH m.uploadedBy WHERE m.isPublic = true ORDER BY m.playCount DESC")
    List<Moodsic> findByIsPublicTrueOrderByPlayCountDesc();

    /**
     * Find moodsics that are either public or uploaded by the specified user.
     * Uses JOIN FETCH for lazy associations to support GraalVM native image.
     */
    @Query("SELECT m FROM Moodsic m JOIN FETCH m.uploadedBy WHERE m.isPublic = true OR m.uploadedBy = :user ORDER BY m.playCount DESC")
    List<Moodsic> findAvailableForUser(@Param("user") User user);

    /**
     * Find a moodsic by ID with associations fetched.
     * Uses JOIN FETCH for lazy associations to support GraalVM native image.
     */
    @Query("SELECT m FROM Moodsic m JOIN FETCH m.uploadedBy WHERE m.id = :id")
    Optional<Moodsic> findByIdWithAssociations(@Param("id") Long id);
}
