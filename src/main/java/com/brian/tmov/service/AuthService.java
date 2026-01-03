package com.brian.tmov.service;

import com.brian.tmov.dto.request.AuthRequest;
import com.brian.tmov.dto.request.GoogleLoginRequest;
import com.brian.tmov.dto.request.UpdateProfileRequest;
import com.brian.tmov.dto.response.AuthResponse;

public interface AuthService {

    void register(AuthRequest request);

    AuthResponse login(AuthRequest request);

    AuthResponse googleLogin(GoogleLoginRequest request);

    AuthResponse getMe(String email);

    AuthResponse updateProfile(String email, UpdateProfileRequest request);
}
