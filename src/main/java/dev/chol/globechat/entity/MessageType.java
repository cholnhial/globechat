package dev.chol.globechat.entity;

/**
 * Types of messages in chat rooms.
 */
public enum MessageType {
    /**
     * Regular chat message from a user.
     */
    CHAT,

    /**
     * User joined the room.
     */
    JOIN,

    /**
     * User left the room.
     */
    LEAVE,

    /**
     * User was kicked from the room.
     */
    KICK,

    /**
     * User was banned from the room.
     */
    BAN,

    /**
     * Moodsic was changed.
     */
    MOODSIC_CHANGE,

    /**
     * Moodsic was paused/resumed.
     */
    MOODSIC_TOGGLE,

    /**
     * Room was destroyed by owner.
     */
    ROOM_DESTROYED,

    /**
     * System notification message.
     */
    SYSTEM
}
