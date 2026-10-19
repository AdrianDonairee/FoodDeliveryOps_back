package com.adriandonaire.fooddeliveryops.auth.dto;

import com.adriandonaire.fooddeliveryops.auth.UserRole;

public record AccountResponse(
        Long id,
        String username,
        UserRole role,
        String address,
        Double latitude,
        Double longitude) {
}