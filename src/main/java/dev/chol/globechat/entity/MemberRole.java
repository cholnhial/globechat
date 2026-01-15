package dev.chol.globechat.entity;

/**
 * Roles that a user can have within a chat room.
 */
public enum MemberRole {
    /**
     * Owner of the room - full control, can ban/kick anyone including mods.
     */
    OWNER,
    
    /**
     * Moderator - can kick/ban regular chatters, but not other mods or owner.
     */
    MOD,
    
    /**
     * Regular chatter - standard participant with no moderation powers.
     */
    CHATTER
}
