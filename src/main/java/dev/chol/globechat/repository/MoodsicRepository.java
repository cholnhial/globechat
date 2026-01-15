package dev.chol.globechat.repository;

import dev.chol.globechat.entity.Moodsic;
import dev.chol.globechat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Moodsic entities.
 */
@Repository
public interface MoodsicRepository extends JpaRepository<Moodsic, Long> {

    /**
     * Find all moodsics uploaded by a specific user.
     */
    List<Moodsic> findByUploadedBy(User uploadedBy);

    /**
     * Find all moodsics ordered by play count (most popular first).
     */
    List<Moodsic> findAllByOrderByPlayCountDesc();

    /**
     * Find top N most popular moodsics.
     */
    List<Moodsic> findTopNByOrderByPlayCountDesc(int n);

    /**
     * Search moodsics by name (case-insensitive).
     */
    @Query("SELECT m FROM Moodsic m WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Moodsic> searchByName(@Param("query") String query);

    /**
     * Increment the play count for a moodsic.
     */
    @Modifying
    @Query("UPDATE Moodsic m SET m.playCount = m.playCount + 1 WHERE m.id = :id")
    void incrementPlayCount(@Param("id") Long id);

    /**
     * Find all public moodsics.
     */
    List<Moodsic> findByIsPublicTrue();

    /**
     * Find all public moodsics ordered by play count.
     */
    List<Moodsic> findByIsPublicTrueOrderByPlayCountDesc();

    /**
     * Find moodsics that are either public or uploaded by the specified user.
     */
    @Query("SELECT m FROM Moodsic m WHERE m.isPublic = true OR m.uploadedBy = :user ORDER BY m.playCount DESC")
    List<Moodsic> findAvailableForUser(@Param("user") User user);
}
