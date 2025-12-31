package com.brian.tmov.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AuthResponse(
        String token,
        String email,
        String displayName,
        String pictureUrl,
        String role,
        String gender,
        LocalDate birthDate,
        String phone,
        String address,
        LocalDateTime createdAt
) {
}
