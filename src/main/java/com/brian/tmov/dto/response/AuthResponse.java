package com.brian.tmov.dto.response;

import java.time.LocalDateTime;

public record AuthResponse(
        String token,
        String email,
        String displayName,
        String pictureUrl,
        String role,
        LocalDateTime createdAt
) {
}
