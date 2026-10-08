package com.adriandonaire.fooddeliveryops.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank(message = "El nombre del cliente es obligatorio.")
        String customerName,

        @NotBlank(message = "El teléfono del cliente es obligatorio.")
        String customerPhone,

        @NotBlank(message = "La dirección del cliente es obligatoria.")
        String customerAddress,

        String customerNotes,

        @NotNull(message = "La latitud GPS es obligatoria.")
        Double customerLat,

        @NotNull(message = "La longitud GPS es obligatoria.")
        Double customerLng,

        @NotEmpty(message = "El pedido debe contener al menos un ítem.")
        @Valid
        List<CreateOrderItemRequest> items
) {
}
