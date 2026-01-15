package dev.chol.globechat.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for JWT authentication.
 */
@ConfigurationProperties(prefix = "globechat.jwt")
public record JwtProperties(
        String secret,
        long expirationMs
) {
}
