package com.brian.tmov.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateProfileRequest(

        String displayName,

        String pictureUrl,

        String oldPassword,

        @Size(min = 8, message = "新密碼長度至少需要8位")
        String newPassword,

        String gender,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate birthDate,

        String phone,

        String address
        ) {
}
