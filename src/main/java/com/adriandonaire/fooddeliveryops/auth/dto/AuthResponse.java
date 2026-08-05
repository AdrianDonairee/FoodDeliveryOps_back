package com.adriandonaire.fooddeliveryops.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        UserResponse user) {
}