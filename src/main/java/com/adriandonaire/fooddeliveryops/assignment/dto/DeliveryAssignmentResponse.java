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
        Double foodShopLat,
        Double foodShopLng,
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
    public static DeliveryAssignmentResponse fromOffer(OrderAssignment assignment) {
        Order order = assignment.getOrder();
        List<DeliveryOfferItemResponse> items = order.getItems().stream()
                .map(DeliveryOfferItemResponse::fromEntity)
                .toList();

        String shopAddress = (order.getFoodShop() != null && order.getFoodShop().getAddress() != null)
                ? order.getFoodShop().getAddress()
                : "";
        Double shopLat = (order.getFoodShop() != null) ? order.getFoodShop().getLatitude() : null;
        Double shopLng = (order.getFoodShop() != null) ? order.getFoodShop().getLongitude() : null;

        return new DeliveryAssignmentResponse(
                order.getId(),
                order.getStatus(),
                order.getFoodShop() != null ? order.getFoodShop().getUsername() : "Comercio",
                shopAddress,
                shopLat,
                shopLng,
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

    public static DeliveryAssignmentResponse fromOffer(OrderAssignment assignment, String ignored) {
        return fromOffer(assignment);
    }

    public static DeliveryAssignmentResponse fromActiveOrder(Order order, Double deliveryEarnings) {
        List<DeliveryOfferItemResponse> items = order.getItems().stream()
                .map(DeliveryOfferItemResponse::fromEntity)
                .toList();

        String acceptedTime = order.getUpdatedAt() != null ? order.getUpdatedAt().toString()
                : (order.getCreatedAt() != null ? order.getCreatedAt().toString() : null);

        String shopAddress = (order.getFoodShop() != null && order.getFoodShop().getAddress() != null)
                ? order.getFoodShop().getAddress()
                : "";
        Double shopLat = (order.getFoodShop() != null) ? order.getFoodShop().getLatitude() : null;
        Double shopLng = (order.getFoodShop() != null) ? order.getFoodShop().getLongitude() : null;

        return new DeliveryAssignmentResponse(
                order.getId(),
                order.getStatus(),
                order.getFoodShop() != null ? order.getFoodShop().getUsername() : "Comercio",
                shopAddress,
                shopLat,
                shopLng,
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

    public static DeliveryAssignmentResponse fromActiveOrder(Order order, Double deliveryEarnings, String ignored) {
        return fromActiveOrder(order, deliveryEarnings);
    }
}
