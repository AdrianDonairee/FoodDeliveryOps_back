package com.adriandonaire.fooddeliveryops.assignment.dto;

import com.adriandonaire.fooddeliveryops.assignment.OrderAssignment;
import com.adriandonaire.fooddeliveryops.order.Order;
import com.adriandonaire.fooddeliveryops.order.OrderStatus;
import java.util.List;

public record DeliveryAssignmentResponse(
        Long orderId,
        OrderStatus status,
        String foodShopName,
        String foodShopAddress,
        String customerName,
        String customerPhone,
        String customerAddress,
        String customerNotes,
        Double customerLat,
        Double customerLng,
        List<DeliveryOfferItemResponse> items,
        Double totalPrice,
        Double deliveryEarnings,
        String expiresAt,
        String acceptedAt
) {
    public static DeliveryAssignmentResponse fromOffer(OrderAssignment assignment, String defaultShopAddress) {
        Order order = assignment.getOrder();
        List<DeliveryOfferItemResponse> items = order.getItems().stream()
                .map(DeliveryOfferItemResponse::fromEntity)
                .toList();

        return new DeliveryAssignmentResponse(
                order.getId(),
                order.getStatus(),
                order.getFoodShop() != null ? order.getFoodShop().getUsername() : "Comercio",
                defaultShopAddress,
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getCustomerAddress(),
                order.getCustomerNotes(),
                order.getCustomerLat(),
                order.getCustomerLng(),
                items,
                order.getTotalPrice(),
                assignment.getDeliveryEarnings(),
                assignment.getExpiresAt() != null ? assignment.getExpiresAt().toString() : null,
                null
        );
    }

    public static DeliveryAssignmentResponse fromActiveOrder(Order order, Double deliveryEarnings, String defaultShopAddress) {
        List<DeliveryOfferItemResponse> items = order.getItems().stream()
                .map(DeliveryOfferItemResponse::fromEntity)
                .toList();

        String acceptedTime = order.getUpdatedAt() != null ? order.getUpdatedAt().toString()
                : (order.getCreatedAt() != null ? order.getCreatedAt().toString() : null);

        return new DeliveryAssignmentResponse(
                order.getId(),
                order.getStatus(),
                order.getFoodShop() != null ? order.getFoodShop().getUsername() : "Comercio",
                defaultShopAddress,
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getCustomerAddress(),
                order.getCustomerNotes(),
                order.getCustomerLat(),
                order.getCustomerLng(),
                items,
                order.getTotalPrice(),
                deliveryEarnings,
                null,
                acceptedTime
        );
    }
}
