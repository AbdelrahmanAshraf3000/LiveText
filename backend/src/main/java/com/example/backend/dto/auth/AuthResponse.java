package com.example.backend.dto.auth;

import com.example.backend.dto.user.UserDto;

public record AuthResponse(UserDto user, String accessToken, String refreshToken) {
}