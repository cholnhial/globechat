# GlobeChat

A chat application where users can find and join chat rooms displayed on a globe. Each room has its own mood music (moodsic), rules, and moderation system.

## Project Purpose

GlobeChat allows users to:
- Create and manage chat rooms with customizable rules
- Find chat rooms geographically on a 3D globe interface
- Set mood music (moodsic) for rooms to create atmosphere
- Join rooms via unique alphanumeric join codes (QR-code friendly)
- Moderate rooms with owner/mod/chatter role hierarchy
- Real-time chat messaging via WebSocket

## Tech Stack

- **Backend**: Spring Boot 4.x, Java 21, Spring Security (JWT), Spring WebSocket
- **Database**: PostgreSQL with Flyway migrations
- **Frontend**: Angular 21 with MapLibre GL for 3D globe, STOMP WebSocket
- **Build**: Maven with frontend-maven-plugin for Angular
- **Testing**: JUnit 5, Mockito, TestContainers

## Project Structure

```
src/main/java/dev/chol/globechat/
├── config/
│   ├── security/      # JWT auth, Spring Security config
│   ├── WebSocketConfig.java
│   └── WebSocketSecurityConfig.java
├── controller/        # REST API + WebSocket endpoints
├── dto/               # Data Transfer Objects
├── entity/            # JPA entities
├── exception/         # Custom exceptions and handlers
├── repository/        # Spring Data JPA repositories
├── service/           # Business logic services
└── util/              # Utility classes

src/main/resources/
├── config/            # Application configuration (YAML)
├── db/migration/      # Flyway SQL migrations
├── static/            # Built frontend assets
└── templates/         # Server-side templates (if any)

src/webapp/src/app/
├── core/
│   ├── models/        # TypeScript interfaces
│   ├── services/      # Angular services (auth, room, chat, moodsic)
│   ├── guards/        # Route guards
│   └── interceptors/  # HTTP interceptors
├── features/
│   ├── auth/          # Login/Register components
│   └── chat/          # Chat UI components
│       ├── chat-layout/
│       ├── room-list/
│       ├── globe/
│       ├── room-info-card/
│       └── chat-window/
└── shared/            # Shared components (toast, etc)

src/test/java/dev/chol/globechat/
├── repository/        # Repository tests (@DataJpaTest)
├── service/           # Service unit tests (Mockito)
└── integration/       # Integration tests (*IT.java)
```

## Domain Model

### Entities

#### User
- **Business Key**: `username` (unique)
- Fields: `id`, `username`, `email`, `passwordHash`, `createdAt`
- Relationships:
  - `ownedRooms`: Rooms this user owns
  - `memberships`: ChatRoomMember records (rooms joined)
  - `uploadedMoodsics`: Moodsics uploaded by user
  - `bansReceived`/`bansIssued`: Ban records

#### ChatRoom
- **Business Key**: `joinCode` (8-char alphanumeric, auto-generated)
- Fields: `id`, `joinCode`, `title`, `description`, `rules` (TEXT), `latitude`, `longitude`, `moodsicPaused`, `createdAt`
- Relationships:
  - `owner`: User who owns the room
  - `currentMoodsic`: Currently playing moodsic (nullable)
  - `members`: ChatRoomMember records
  - `bans`: RoomBan records
  - `messages`: ChatMessage records

#### ChatMessage
- Fields: `id`, `content`, `messageType`, `createdAt`
- Relationships: `chatRoom`, `sender`
- **MessageType**: CHAT, JOIN, LEAVE, KICK, BAN, MOODSIC_CHANGE, MOODSIC_TOGGLE, ROOM_DESTROYED, SYSTEM

#### ChatRoomMember
- **Composite Key**: `(userId, chatRoomId)`
- Fields: `role` (OWNER/MOD/CHATTER), `joinedAt`
- Represents a user's membership in a room with their role

#### Moodsic
- **Business Key**: `id` (system-generated)
- Fields: `id`, `name`, `filePath`, `contentType`, `playCount`, `isPublic`, `createdAt`
- Relationships:
  - `uploadedBy`: User who uploaded
  - `activeInRooms`: Rooms currently playing this moodsic
- **Storage**: Files stored on disk at configured path; `filePath` is relative
- **Visibility**: Public moodsics visible to all; private only to uploader

#### RoomBan
- Fields: `id`, `reason`, `bannedAt`
- Relationships: `chatRoom`, `bannedUser`, `bannedBy`
- Unique constraint on `(chatRoom, bannedUser)`

#### MemberRole (Enum)
- `OWNER` - Full control, can ban/kick anyone
- `MOD` - Can ban/kick CHATTERs only
- `CHATTER` - Regular participant

### Entity Relationships Diagram

```
User 1──* ChatRoom (owner)
User 1──* ChatRoomMember
User 1──* Moodsic (uploadedBy)
User 1──* RoomBan (bannedUser, bannedBy)

ChatRoom 1──* ChatRoomMember
ChatRoom 1──* RoomBan
ChatRoom *──1 Moodsic (currentMoodsic, nullable)

ChatRoomMember *──1 User
ChatRoomMember *──1 ChatRoom
```

## API Endpoints

### Authentication (`/api/auth`) - No auth required

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/register` | Register new user |
| POST | `/login` | Login, returns JWT |

### Users (`/api/users`) - Auth required

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/me` | Get current user profile |

### Rooms (`/api/rooms`) - Auth required

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create room (with lat/lng) |
| GET | `/{joinCode}` | Get room details |
| PUT | `/{joinCode}` | Update room (owner) |
| DELETE | `/{joinCode}` | Delete room (owner) |
| POST | `/{joinCode}/join` | Join room |
| POST | `/{joinCode}/leave` | Leave room |
| GET | `/{joinCode}/members` | List members (member) |
| GET | `/{joinCode}/role` | Get user's role in room |
| GET | `/{joinCode}/messages` | Get chat messages |
| POST | `/{joinCode}/kick/{username}` | Kick user (mod/owner) |
| POST | `/{joinCode}/ban` | Ban user (mod/owner) |
| DELETE | `/{joinCode}/ban/{username}` | Unban user (mod/owner) |
| POST | `/{joinCode}/mods/{username}` | Promote to mod (owner) |
| DELETE | `/{joinCode}/mods/{username}` | Demote mod (owner) |
| PUT | `/{joinCode}/moodsic` | Set room moodsic (owner) |
| DELETE | `/{joinCode}/moodsic` | Clear room moodsic (owner) |
| POST | `/{joinCode}/moodsic/toggle` | Toggle moodsic pause (mod/owner) |
| GET | `/my` | Get user's joined rooms |
| GET | `/markers` | Get all room markers for globe |

### WebSocket (`/ws`) - STOMP over SockJS

| Destination | Type | Description |
|-------------|------|-------------|
| `/app/chat/{joinCode}/send` | Send | Send message to room |
| `/app/chat/{joinCode}/join` | Send | Notify room of user join |
| `/app/chat/{joinCode}/leave` | Send | Notify room of user leave |
| `/topic/room/{joinCode}` | Subscribe | Receive room messages |

### Moodsics (`/api/moodsics`) - Auth required

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Upload moodsic (multipart) |
| GET | `/` | List available moodsics |
| GET | `/public` | List public moodsics |
| GET | `/my` | List user's moodsics |
| GET | `/{id}` | Get moodsic details |
| PATCH | `/{id}/visibility` | Toggle public/private |
| DELETE | `/{id}` | Delete moodsic |

## Security

- JWT-based authentication (24h expiration)
- BCrypt password hashing
- All `/api/**` endpoints require authentication except `/api/auth/**`
- Role-based access within rooms (OWNER > MOD > CHATTER)

## Conventions

### Entities
- Use Lombok `@Getter`, `@Setter`, `@NoArgsConstructor`
- Custom `equals()`/`hashCode()` using business identifier
- `@PrePersist` for auto-setting `createdAt` timestamps
- Composite keys use `@EmbeddedId` pattern

### Repositories
- Extend `JpaRepository<Entity, IdType>`
- Use derived query methods where possible
- Custom `@Query` for complex queries
- Include `existsBy*` methods for validation checks

### DTOs
- Use Java records for immutability
- Include static `from(Entity)` factory methods
- Validation annotations on request DTOs

### Database
- Use Flyway migrations (V1, V2, V3...)
- PostgreSQL with `BIGSERIAL` for IDs
- Timestamps use `TIMESTAMP WITH TIME ZONE`
- Foreign keys with appropriate `ON DELETE` behavior

## Configuration

### application.yaml
```yaml
globechat:
  jwt:
    secret: ${JWT_SECRET:dev-secret-key...}
    expiration-ms: 86400000  # 24 hours
  moodsic:
    storage-path: ./moodsic-uploads

spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
```

## Development Commands

```bash
# Run with dev profile
./mvnw spring-boot:run -Pdev

# Run unit tests only
./mvnw test

# Run integration tests only
./mvnw verify -DskipUTs

# Run all tests
./mvnw verify

# Build for production
./mvnw clean package -Pprod

# Run with Docker Compose (PostgreSQL)
docker compose up -d
./mvnw spring-boot:run
```

## Testing Strategy

- **Repository tests**: `@DataJpaTest` with TestContainers PostgreSQL
- **Service tests**: Unit tests with `@MockBean` repositories
- **Integration tests**: `@SpringBootTest` with TestContainers, named `*IT.java`
- Surefire runs unit tests, Failsafe runs integration tests

## Future Features (Roadmap)

- [ ] Real-time chat messaging (WebSocket)
- [ ] Globe visualization for room discovery
- [ ] QR code generation for join codes
- [ ] Moodsic playback and streaming
- [ ] Room search and filtering
- [ ] User profiles and avatars
- [ ] Room activity metrics (member count, message count)
- [ ] Admin dashboard for platform moderation
