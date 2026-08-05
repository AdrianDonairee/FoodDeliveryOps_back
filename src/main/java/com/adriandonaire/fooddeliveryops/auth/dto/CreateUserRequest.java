package com.adriandonaire.fooddeliveryops.auth.dto;

import com.adriandonaire.fooddeliveryops.auth.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank String password,
        @NotNull UserRole role) {
}