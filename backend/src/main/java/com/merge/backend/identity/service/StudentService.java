package com.merge.backend.identity.service;

import com.merge.backend.identity.dto.*;

public interface StudentService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(String refreshToken);
    void logout(String accessToken);
    StudentResponse getProfile(String email);
    StudentResponse updateProfile(String email, UpdateProfileRequest request);
}
