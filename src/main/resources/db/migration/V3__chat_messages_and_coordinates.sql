-- Add geo coordinates to chat_rooms
ALTER TABLE chat_rooms ADD COLUMN latitude DOUBLE PRECISION NOT NULL DEFAULT 0;
ALTER TABLE chat_rooms ADD COLUMN longitude DOUBLE PRECISION NOT NULL DEFAULT 0;
ALTER TABLE chat_rooms ADD COLUMN moodsic_paused BOOLEAN NOT NULL DEFAULT false;

-- Create index for geo queries
CREATE INDEX idx_chat_rooms_coordinates ON chat_rooms(latitude, longitude);

-- Chat messages table
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    chat_room_id BIGINT NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    message_type VARCHAR(20) NOT NULL DEFAULT 'CHAT',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chat_messages_room ON chat_messages(chat_room_id);
CREATE INDEX idx_chat_messages_created ON chat_messages(created_at DESC);
