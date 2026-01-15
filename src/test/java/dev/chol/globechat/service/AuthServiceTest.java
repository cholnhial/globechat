package dev.chol.globechat.service;

import dev.chol.globechat.config.security.JwtTokenProvider;
import dev.chol.globechat.dto.AuthResponse;
import dev.chol.globechat.dto.LoginRequest;
import dev.chol.globechat.dto.RegisterRequest;
import dev.chol.globechat.entity.User;
import dev.chol.globechat.exception.BadRequestException;
import dev.chol.globechat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("testuser", "test@example.com", "password123");
        loginRequest = new LoginRequest("test@example.com", "password123");

        testUser = new User("testuser", "test@example.com", "hashedpassword");
        testUser.setId(1L);
    }

    @Test
    void register_withValidData_createsUserAndReturnsToken() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(tokenProvider.generateToken("testuser")).thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().username()).isEqualTo("testuser");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_withExistingUsername_throwsException() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Username is already taken");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_withExistingEmail_throwsException() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email is already registered");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_withValidCredentials_returnsToken() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(null, null));
        when(tokenProvider.generateToken(any(UsernamePasswordAuthenticationToken.class))).thenReturn("jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().email()).isEqualTo("test@example.com");
    }

    @Test
    void login_withInvalidEmail_throwsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid email or password");
    }
}
