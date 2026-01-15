package dev.chol.globechat.repository;

import dev.chol.globechat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their unique username.
     */
    Optional<User> findByUsername(String username);

    /**
     * Find a user by their email address.
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a username is already taken.
     */
    boolean existsByUsername(String username);

    /**
     * Check if an email is already registered.
     */
    boolean existsByEmail(String email);
}
