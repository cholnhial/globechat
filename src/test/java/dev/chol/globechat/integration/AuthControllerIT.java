package dev.chol.globechat.integration;

import dev.chol.globechat.TestcontainersConfiguration;
import dev.chol.globechat.dto.*;
import dev.chol.globechat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AuthControllerIT {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        userRepository.deleteAll();
    }

    @Test
    void register_withValidData_createsUserAndReturnsToken() {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password123");

        AuthResponse response = webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.token()).isNotBlank();
        assertThat(response.user().username()).isEqualTo("testuser");
    }

    @Test
    void register_withDuplicateUsername_returnsBadRequest() {
        RegisterRequest request1 = new RegisterRequest("testuser", "test1@example.com", "password123");
        webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request1)
                .exchange()
                .expectStatus().isCreated();

        RegisterRequest request2 = new RegisterRequest("testuser", "test2@example.com", "password123");
        webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request2)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void register_withDuplicateEmail_returnsBadRequest() {
        RegisterRequest request1 = new RegisterRequest("testuser1", "test@example.com", "password123");
        webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request1)
                .exchange()
                .expectStatus().isCreated();

        RegisterRequest request2 = new RegisterRequest("testuser2", "test@example.com", "password123");
        webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request2)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void register_withInvalidEmail_returnsBadRequest() {
        RegisterRequest request = new RegisterRequest("testuser", "invalid-email", "password123");

        webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void register_withShortPassword_returnsBadRequest() {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "short");

        webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void login_withValidCredentials_returnsToken() {
        // Register first
        RegisterRequest registerRequest = new RegisterRequest("testuser", "test@example.com", "password123");
        webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isCreated();

        // Login
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");
        AuthResponse response = webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.token()).isNotBlank();
    }

    @Test
    void login_withInvalidPassword_returnsUnauthorized() {
        // Register first
        RegisterRequest registerRequest = new RegisterRequest("testuser", "test@example.com", "password123");
        webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isCreated();

        // Login with wrong password
        LoginRequest loginRequest = new LoginRequest("test@example.com", "wrongpassword");
        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void login_withNonExistentEmail_returnsBadRequest() {
        LoginRequest loginRequest = new LoginRequest("nonexistent@example.com", "password123");

        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
