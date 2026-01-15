package dev.chol.globechat.service;

import dev.chol.globechat.config.security.JwtTokenProvider;
import dev.chol.globechat.config.security.UserDetailsServiceImpl;
import dev.chol.globechat.dto.AuthResponse;
import dev.chol.globechat.dto.LoginRequest;
import dev.chol.globechat.dto.RegisterRequest;
import dev.chol.globechat.dto.UserDto;
import dev.chol.globechat.entity.User;
import dev.chol.globechat.exception.BadRequestException;
import dev.chol.globechat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user authentication and registration.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Register a new user.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username is already taken");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email is already registered");
        }

        User user = new User(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password())
        );

        user = userRepository.save(user);

        String token = tokenProvider.generateToken(user.getUsername());

        return new AuthResponse(token, UserDto.from(user));
    }

    /**
     * Authenticate a user and return a JWT token.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // First, find the user by email to get their username
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        // Authenticate using username and password
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), request.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = tokenProvider.generateToken(authentication);

        return new AuthResponse(token, UserDto.from(user));
    }
}
