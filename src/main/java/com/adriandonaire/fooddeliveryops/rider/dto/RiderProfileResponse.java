package com.adriandonaire.fooddeliveryops.rider.dto;

import com.adriandonaire.fooddeliveryops.assignment.dto.DeliveryAssignmentResponse;
import com.adriandonaire.fooddeliveryops.order.OrderStatus;
import com.adriandonaire.fooddeliveryops.order.dto.OrderResponse;
import com.adriandonaire.fooddeliveryops.rider.AvailabilityStatus;
import com.adriandonaire.fooddeliveryops.rider.DutyStatus;

public record RiderProfileResponse(
        Long id,
        String username,
        DutyStatus dutyStatus,
        AvailabilityStatus availabilityStatus,
        Double currentLat,
        Double currentLng,
        String lastLocationUpdatedAt,
        long deliveriesCountToday,
        double earningsToday,
        DeliveryAssignmentResponse activeDelivery,
        Long orderId,
        OrderStatus orderStatus,
        OrderResponse order
) {
}
