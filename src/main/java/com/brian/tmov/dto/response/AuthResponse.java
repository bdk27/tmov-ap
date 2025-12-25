package com.brian.tmov.dto.response;

public record AuthResponse(
        String token,
        String email,
        String displayName,
        String pictureUrl,
        String role
) {
}
