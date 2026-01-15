package dev.chol.globechat.service;

import dev.chol.globechat.config.security.UserPrincipal;
import dev.chol.globechat.dto.UserDto;
import dev.chol.globechat.entity.User;
import dev.chol.globechat.exception.ResourceNotFoundException;
import dev.chol.globechat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user operations.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Get the currently authenticated user.
     */
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));
    }

    /**
     * Get the currently authenticated user, or null if not authenticated.
     */
    @Transactional(readOnly = true)
    public User getCurrentUserOrNull() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            return null;
        }
        return userRepository.findById(userPrincipal.getId()).orElse(null);
    }

    /**
     * Get user DTO for the currently authenticated user.
     */
    @Transactional(readOnly = true)
    public UserDto getCurrentUserDto() {
        return UserDto.from(getCurrentUser());
    }

    /**
     * Find a user by username.
     */
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    /**
     * Find a user by ID.
     */
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
}
