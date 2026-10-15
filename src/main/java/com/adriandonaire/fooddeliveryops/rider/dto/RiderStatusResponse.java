package com.adriandonaire.fooddeliveryops.rider.dto;

import com.adriandonaire.fooddeliveryops.rider.AvailabilityStatus;
import com.adriandonaire.fooddeliveryops.rider.DutyStatus;
import com.adriandonaire.fooddeliveryops.rider.RiderStatus;
import java.time.Instant;

public record RiderStatusResponse(
        Long riderId,
        String riderUsername,
        DutyStatus dutyStatus,
        AvailabilityStatus availabilityStatus,
        Double currentLat,
        Double currentLng,
        Instant lastLocationUpdatedAt,
        Instant updatedAt
) {
    public static RiderStatusResponse fromEntity(RiderStatus status) {
        return new RiderStatusResponse(
                status.getRider().getId(),
                status.getRider().getUsername(),
                status.getDutyStatus(),
                status.getAvailabilityStatus(),
                status.getCurrentLat(),
                status.getCurrentLng(),
                status.getLastLocationUpdatedAt(),
                status.getUpdatedAt()
        );
    }
}
