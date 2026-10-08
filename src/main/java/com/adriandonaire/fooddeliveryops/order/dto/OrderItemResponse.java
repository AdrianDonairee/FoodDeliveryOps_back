package com.adriandonaire.fooddeliveryops.order.dto;

import com.adriandonaire.fooddeliveryops.order.OrderItem;

public record OrderItemResponse(
        Long id,
        String name,
        Integer quantity,
        Double price
) {
    public static OrderItemResponse fromEntity(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getName(),
                item.getQuantity(),
                item.getPrice()
        );
    }
}
