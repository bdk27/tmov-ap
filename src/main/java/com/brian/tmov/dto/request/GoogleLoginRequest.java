package com.brian.tmov.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(

        @NotBlank(message = "ID Token 不能為空")
        String idToken
) {
}
