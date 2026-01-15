package dev.chol.globechat.dto;

import java.time.Instant;
import java.util.List;

/**
 * Standard API error response.
 */
public record ErrorResponse(
        int status,
        String message,
        List<String> errors,
        Instant timestamp
) {
    public ErrorResponse(int status, String message) {
        this(status, message, List.of(), Instant.now());
    }

    public ErrorResponse(int status, String message, List<String> errors) {
        this(status, message, errors, Instant.now());
    }
}
