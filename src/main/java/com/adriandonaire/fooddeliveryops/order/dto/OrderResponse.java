package com.adriandonaire.fooddeliveryops.order.dto;

import com.adriandonaire.fooddeliveryops.order.Order;
import com.adriandonaire.fooddeliveryops.order.OrderStatus;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long foodShopId,
        String foodShopName,
        String customerName,
        String customerPhone,
        String customerAddress,
        String customerNotes,
        Double customerLat,
        Double customerLng,
        List<OrderItemResponse> items,
        Double totalPrice,
        OrderStatus status,
        Long assignedRiderId,
        String assignedRiderName,
        Instant createdAt
) {
    public static OrderResponse fromEntity(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(OrderItemResponse::fromEntity)
                .toList();

        Long riderId = order.getAssignedRider() != null ? order.getAssignedRider().getId() : null;
        String riderName = order.getAssignedRider() != null ? order.getAssignedRider().getUsername() : null;

        return new OrderResponse(
                order.getId(),
                order.getFoodShop().getId(),
                order.getFoodShop().getUsername(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getCustomerAddress(),
                order.getCustomerNotes(),
                order.getCustomerLat(),
                order.getCustomerLng(),
                itemResponses,
                order.getTotalPrice(),
                order.getStatus(),
                riderId,
                riderName,
                order.getCreatedAt()
        );
    }
}
