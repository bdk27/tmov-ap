package com.brian.tmov.service;

import com.brian.tmov.dto.request.AuthRequest;
import com.brian.tmov.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(AuthRequest request);

    AuthResponse login(AuthRequest request);
}
