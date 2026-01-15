-- Core entities for GlobeChat application
-- V2: Users, Moodsics, Chat Rooms, Members, and Bans

-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);

-- Moodsics table (mood music tracks stored on disk)
CREATE TABLE moodsics (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    uploaded_by_id BIGINT NOT NULL REFERENCES users(id),
    play_count BIGINT NOT NULL DEFAULT 0,
    is_public BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_moodsics_uploaded_by ON moodsics(uploaded_by_id);
CREATE INDEX idx_moodsics_play_count ON moodsics(play_count DESC);
CREATE INDEX idx_moodsics_public ON moodsics(is_public) WHERE is_public = true;

-- Chat rooms table
CREATE TABLE chat_rooms (
    id BIGSERIAL PRIMARY KEY,
    join_code VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    rules TEXT,
    owner_id BIGINT NOT NULL REFERENCES users(id),
    current_moodsic_id BIGINT REFERENCES moodsics(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chat_rooms_join_code ON chat_rooms(join_code);
CREATE INDEX idx_chat_rooms_owner ON chat_rooms(owner_id);
CREATE INDEX idx_chat_rooms_moodsic ON chat_rooms(current_moodsic_id);

-- Chat room members (join table with role)
CREATE TABLE chat_room_members (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    chat_room_id BIGINT NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, chat_room_id),
    CONSTRAINT chk_member_role CHECK (role IN ('OWNER', 'MOD', 'CHATTER'))
);

CREATE INDEX idx_chat_room_members_room ON chat_room_members(chat_room_id);
CREATE INDEX idx_chat_room_members_user ON chat_room_members(user_id);

-- Room bans
CREATE TABLE room_bans (
    id BIGSERIAL PRIMARY KEY,
    chat_room_id BIGINT NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    banned_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    banned_by_id BIGINT NOT NULL REFERENCES users(id),
    reason VARCHAR(500),
    banned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (chat_room_id, banned_user_id)
);

CREATE INDEX idx_room_bans_room ON room_bans(chat_room_id);
CREATE INDEX idx_room_bans_user ON room_bans(banned_user_id);
