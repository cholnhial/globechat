package dev.chol.globechat.integration;

import dev.chol.globechat.TestcontainersConfiguration;
import dev.chol.globechat.dto.*;
import dev.chol.globechat.repository.ChatRoomMemberRepository;
import dev.chol.globechat.repository.ChatRoomRepository;
import dev.chol.globechat.repository.RoomBanRepository;
import dev.chol.globechat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class RoomControllerIT {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository roomRepository;

    @Autowired
    private ChatRoomMemberRepository memberRepository;

    @Autowired
    private RoomBanRepository banRepository;

    private String ownerToken;
    private String memberToken;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        banRepository.deleteAll();
        memberRepository.deleteAll();
        roomRepository.deleteAll();
        userRepository.deleteAll();

        // Register owner
        RegisterRequest ownerRequest = new RegisterRequest("owner", "owner@example.com", "password123");
        AuthResponse ownerResponse = webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ownerRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();
        ownerToken = ownerResponse.token();

        // Register member
        RegisterRequest memberRequest = new RegisterRequest("member", "member@example.com", "password123");
        AuthResponse memberResponse = webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(memberRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();
        memberToken = memberResponse.token();
    }

    @Test
    void createRoom_withValidData_createsRoom() {
        CreateRoomRequest request = new CreateRoomRequest("Test Room", "Description", "• Rule 1\n• Rule 2", 40.7128, -74.0060);

        RoomDto response = webTestClient.post()
                .uri("/api/rooms")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(RoomDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Test Room");
        assertThat(response.joinCode()).isNotBlank();
        assertThat(response.memberCount()).isEqualTo(1);
    }

    @Test
    void createRoom_withoutAuth_returnsUnauthorized() {
        CreateRoomRequest request = new CreateRoomRequest("Test Room", "Description", null, 40.7128, -74.0060);

        webTestClient.post()
                .uri("/api/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getRoom_whenExists_returnsRoom() {
        // Create room
        CreateRoomRequest createRequest = new CreateRoomRequest("Test Room", "Description", null, 40.7128, -74.0060);
        RoomDto created = webTestClient.post()
                .uri("/api/rooms")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(RoomDto.class)
                .returnResult()
                .getResponseBody();

        // Get room
        RoomDto response = webTestClient.get()
                .uri("/api/rooms/" + created.joinCode())
                .header("Authorization", "Bearer " + ownerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(RoomDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(response.title()).isEqualTo("Test Room");
    }

    @Test
    void joinRoom_whenNotMember_addsMember() {
        // Create room as owner
        CreateRoomRequest createRequest = new CreateRoomRequest("Test Room", "Description", null, 40.7128, -74.0060);
        RoomDto created = webTestClient.post()
                .uri("/api/rooms")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(RoomDto.class)
                .returnResult()
                .getResponseBody();

        // Join as member
        RoomDto response = webTestClient.post()
                .uri("/api/rooms/" + created.joinCode() + "/join")
                .header("Authorization", "Bearer " + memberToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(RoomDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(response.memberCount()).isEqualTo(2);
    }

    @Test
    void kickUser_asOwner_kicksUser() {
        // Create room
        CreateRoomRequest createRequest = new CreateRoomRequest("Test Room", "Description", null, 40.7128, -74.0060);
        RoomDto created = webTestClient.post()
                .uri("/api/rooms")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(RoomDto.class)
                .returnResult()
                .getResponseBody();

        // Member joins
        webTestClient.post()
                .uri("/api/rooms/" + created.joinCode() + "/join")
                .header("Authorization", "Bearer " + memberToken)
                .exchange()
                .expectStatus().isOk();

        // Owner kicks member
        webTestClient.post()
                .uri("/api/rooms/" + created.joinCode() + "/kick/member")
                .header("Authorization", "Bearer " + ownerToken)
                .exchange()
                .expectStatus().isNoContent();

        // Verify member count
        RoomDto room = webTestClient.get()
                .uri("/api/rooms/" + created.joinCode())
                .header("Authorization", "Bearer " + ownerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(RoomDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(room.memberCount()).isEqualTo(1);
    }

    @Test
    void banUser_asOwner_bansUser() {
        // Create room
        CreateRoomRequest createRequest = new CreateRoomRequest("Test Room", "Description", null, 40.7128, -74.0060);
        RoomDto created = webTestClient.post()
                .uri("/api/rooms")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(RoomDto.class)
                .returnResult()
                .getResponseBody();

        // Member joins
        webTestClient.post()
                .uri("/api/rooms/" + created.joinCode() + "/join")
                .header("Authorization", "Bearer " + memberToken)
                .exchange()
                .expectStatus().isOk();

        // Owner bans member
        BanRequest banRequest = new BanRequest("member", "Breaking rules");
        webTestClient.post()
                .uri("/api/rooms/" + created.joinCode() + "/ban")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(banRequest)
                .exchange()
                .expectStatus().isNoContent();

        // Try to join again - should fail
        webTestClient.post()
                .uri("/api/rooms/" + created.joinCode() + "/join")
                .header("Authorization", "Bearer " + memberToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void deleteRoom_asOwner_deletesRoom() {
        // Create room
        CreateRoomRequest createRequest = new CreateRoomRequest("Test Room", "Description", null, 40.7128, -74.0060);
        RoomDto created = webTestClient.post()
                .uri("/api/rooms")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(RoomDto.class)
                .returnResult()
                .getResponseBody();

        // Delete room
        webTestClient.delete()
                .uri("/api/rooms/" + created.joinCode())
                .header("Authorization", "Bearer " + ownerToken)
                .exchange()
                .expectStatus().isNoContent();

        // Verify room doesn't exist
        webTestClient.get()
                .uri("/api/rooms/" + created.joinCode())
                .header("Authorization", "Bearer " + ownerToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteRoom_asNonOwner_returnsForbidden() {
        // Create room
        CreateRoomRequest createRequest = new CreateRoomRequest("Test Room", "Description", null, 40.7128, -74.0060);
        RoomDto created = webTestClient.post()
                .uri("/api/rooms")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(RoomDto.class)
                .returnResult()
                .getResponseBody();

        // Try to delete as member
        webTestClient.delete()
                .uri("/api/rooms/" + created.joinCode())
                .header("Authorization", "Bearer " + memberToken)
                .exchange()
                .expectStatus().isForbidden();
    }
}
