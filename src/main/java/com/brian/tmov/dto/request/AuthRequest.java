package com.brian.tmov.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(

        @NotBlank(message = "帳號不能為空")
        @Email(message = "Email格式不正確")
        String email,

        @NotBlank(message = "密碼不能為空")
        @Size(min = 8, message = "密碼長度至少需要8位")
        String password,

        Boolean rememberMe
) {

    public boolean isRememberMe() {
        return rememberMe != null && rememberMe;
    }
}
