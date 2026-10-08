package com.adriandonaire.fooddeliveryops.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderItemRequest(
        @NotBlank(message = "El nombre del ítem es obligatorio.")
        String name,

        @NotNull(message = "La cantidad es obligatoria.")
        @Min(value = 1, message = "La cantidad debe ser al menos 1.")
        Integer quantity,

        @NotNull(message = "El precio es obligatorio.")
        @Positive(message = "El precio debe ser mayor a cero.")
        Double price
) {
}
