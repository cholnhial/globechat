package dev.chol.globechat.integration;

import dev.chol.globechat.TestcontainersConfiguration;
import dev.chol.globechat.dto.*;
import dev.chol.globechat.repository.MoodsicRepository;
import dev.chol.globechat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class MoodsicControllerIT {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("globechat.moodsic.storage-path", () -> tempDir.toString());
    }

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MoodsicRepository moodsicRepository;

    private String userToken;
    private String otherUserToken;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        moodsicRepository.deleteAll();
        userRepository.deleteAll();

        // Register user
        RegisterRequest userRequest = new RegisterRequest("uploader", "uploader@example.com", "password123");
        AuthResponse userResponse = webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();
        userToken = userResponse.token();

        // Register other user
        RegisterRequest otherRequest = new RegisterRequest("other", "other@example.com", "password123");
        AuthResponse otherResponse = webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(otherRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();
        otherUserToken = otherResponse.token();
    }

    @Test
    void upload_withoutAuth_returnsUnauthorized() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new ByteArrayResource("fake audio".getBytes()) {
            @Override
            public String getFilename() {
                return "song.mp3";
            }
        }).contentType(MediaType.parseMediaType("audio/mpeg"));
        builder.part("name", "My Song");

        webTestClient.post()
                .uri("/api/moodsics")
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void toggleVisibility_asNonOwner_returnsForbidden() {
        // Upload a moodsic as user
        MoodsicDto moodsic = uploadMoodsic(userToken, "Test Song", true);

        // Try to toggle as other user
        webTestClient.patch()
                .uri("/api/moodsics/" + moodsic.id() + "/visibility")
                .header("Authorization", "Bearer " + otherUserToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void delete_asOwner_deletesMoodsic() {
        // Upload a moodsic
        MoodsicDto moodsic = uploadMoodsic(userToken, "Test Song", true);

        // Delete it
        webTestClient.delete()
                .uri("/api/moodsics/" + moodsic.id())
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isNoContent();

        // Verify it's deleted
        webTestClient.get()
                .uri("/api/moodsics/" + moodsic.id())
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    private MoodsicDto uploadMoodsic(String token, String name, boolean isPublic) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new ByteArrayResource("fake audio content".getBytes()) {
            @Override
            public String getFilename() {
                return "song.mp3";
            }
        }).contentType(MediaType.parseMediaType("audio/mpeg"));
        builder.part("name", name);
        builder.part("isPublic", String.valueOf(isPublic));

        return webTestClient.post()
                .uri("/api/moodsics")
                .header("Authorization", "Bearer " + token)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(MoodsicDto.class)
                .returnResult()
                .getResponseBody();
    }
}
