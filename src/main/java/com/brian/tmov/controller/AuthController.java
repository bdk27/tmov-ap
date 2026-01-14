package com.brian.tmov.controller;

import com.brian.tmov.dto.request.AuthRequest;
import com.brian.tmov.dto.request.GoogleLoginRequest;
import com.brian.tmov.dto.request.UpdateProfileRequest;
import com.brian.tmov.dto.response.AuthResponse;
import com.brian.tmov.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@Tag(name = "身份驗證", description = "負責會員註冊、登入、第三方登入與個人資料管理")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(summary = "會員註冊", description = "建立新的會員帳號 (Email 為唯一識別)")
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest request) {
        authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("message", "註冊成功，請重新登入"));
    }

    @Operation(summary = "會員登入", description = "使用 Email 與密碼登入，成功後回傳 JWT Token")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Google 登入", description = "接收前端傳來的 Google ID Token 進行驗證與登入/註冊")
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.googleLogin(request));
    }

    @Operation(summary = "取得個人資料", description = "取得當前登入使用者的詳細資訊")
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getMe(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(authService.getMe(principal.getName()));
    }

    @Operation(summary = "更新個人資料", description = "修改暱稱、頭像、密碼或詳細個資")
    @PutMapping("/me")
    public ResponseEntity<AuthResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Principal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AuthResponse response = authService.updateProfile(principal.getName(), request);
        return ResponseEntity.ok(response);
    }
}
