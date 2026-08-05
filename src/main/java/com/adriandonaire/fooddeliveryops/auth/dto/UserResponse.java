package com.adriandonaire.fooddeliveryops.auth.dto;

import com.adriandonaire.fooddeliveryops.auth.UserRole;

public record UserResponse(
        String username,
        UserRole role) {
}