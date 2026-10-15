package com.adriandonaire.fooddeliveryops.rider.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record UpdateLocationRequest(
        @NotNull(message = "La latitud es obligatoria.")
        @DecimalMin(value = "-90.0", message = "La latitud debe ser mayor o igual a -90.0")
        @DecimalMax(value = "90.0", message = "La latitud debe ser menor o igual a 90.0")
        Double latitude,

        @NotNull(message = "La longitud es obligatoria.")
        @DecimalMin(value = "-180.0", message = "La longitud debe ser mayor o igual a -180.0")
        @DecimalMax(value = "180.0", message = "La longitud debe ser menor o igual a 180.0")
        Double longitude,

        String timestamp
) {}
