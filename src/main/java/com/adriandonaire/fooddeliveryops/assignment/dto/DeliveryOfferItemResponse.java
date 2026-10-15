package com.adriandonaire.fooddeliveryops.assignment.dto;

import com.adriandonaire.fooddeliveryops.order.OrderItem;

public record DeliveryOfferItemResponse(
        String name,
        Integer quantity,
        Double price
) {
    public static DeliveryOfferItemResponse fromEntity(OrderItem item) {
        return new DeliveryOfferItemResponse(item.getName(), item.getQuantity(), item.getPrice());
    }
}
