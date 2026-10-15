package com.adriandonaire.fooddeliveryops.rider.dto;

import com.adriandonaire.fooddeliveryops.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDeliveryStatusRequest(
        @NotNull(message = "El nuevo estado de la entrega es obligatorio.")
        OrderStatus status
) {
}
