package com.merge.backend.identity.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private StudentResponse student;

    public AuthResponse(String accessToken, String refreshToken, StudentResponse student) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.student = student;
    }
}
