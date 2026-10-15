package com.adriandonaire.fooddeliveryops.rider.service;

import com.adriandonaire.fooddeliveryops.auth.AppUser;
import com.adriandonaire.fooddeliveryops.auth.UserRole;
import com.adriandonaire.fooddeliveryops.auth.repository.AppUserRepository;
import com.adriandonaire.fooddeliveryops.rider.AvailabilityStatus;
import com.adriandonaire.fooddeliveryops.rider.DutyStatus;
import com.adriandonaire.fooddeliveryops.rider.RiderStatus;
import com.adriandonaire.fooddeliveryops.rider.dto.RiderStatusResponse;
import com.adriandonaire.fooddeliveryops.rider.dto.UpdateDutyStatusRequest;
import com.adriandonaire.fooddeliveryops.rider.dto.UpdateLocationRequest;
import com.adriandonaire.fooddeliveryops.rider.repository.RiderStatusRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiderService {

    private final RiderStatusRepository riderStatusRepository;
    private final AppUserRepository appUserRepository;

    public RiderService(RiderStatusRepository riderStatusRepository, AppUserRepository appUserRepository) {
        this.riderStatusRepository = riderStatusRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public RiderStatus getOrCreateRiderStatusEntity(AppUser rider) {
        return riderStatusRepository.findByRiderId(rider.getId())
                .orElseGet(() -> {
                    RiderStatus newStatus = new RiderStatus(rider);
                    return riderStatusRepository.save(newStatus);
                });
    }

    @Transactional(readOnly = true)
    public RiderStatusResponse getRiderStatus(String username) {
        AppUser rider = getRiderUser(username);
        RiderStatus status = riderStatusRepository.findByRiderId(rider.getId())
                .orElseGet(() -> new RiderStatus(rider));
        return RiderStatusResponse.fromEntity(status);
    }

    @Transactional
    public RiderStatusResponse updateDutyStatus(String username, UpdateDutyStatusRequest request) {
        AppUser rider = getRiderUser(username);
        RiderStatus status = getOrCreateRiderStatusEntity(rider);

        DutyStatus newDuty = request.dutyStatus();

        if (newDuty == DutyStatus.NO_TRABAJANDO && status.getAvailabilityStatus() == AvailabilityStatus.OCUPADO) {
            throw new IllegalStateException("No puedes cambiar a NO_TRABAJANDO mientras tengas un pedido asignado o en curso.");
        }

        status.setDutyStatus(newDuty);

        if (newDuty == DutyStatus.NO_TRABAJANDO) {
            status.setAvailabilityStatus(AvailabilityStatus.LIBRE);
        }

        RiderStatus saved = riderStatusRepository.save(status);
        return RiderStatusResponse.fromEntity(saved);
    }

    @Transactional
    public RiderStatusResponse updateLocation(String username, UpdateLocationRequest request) {
        AppUser rider = getRiderUser(username);
        RiderStatus status = getOrCreateRiderStatusEntity(rider);

        if (status.getDutyStatus() != DutyStatus.TRABAJANDO) {
            throw new IllegalStateException("El repartidor debe estar en estado TRABAJANDO para enviar su ubicación GPS.");
        }

        status.setCurrentLat(request.latitude());
        status.setCurrentLng(request.longitude());

        Instant locationTime = Instant.now();
        if (request.timestamp() != null && !request.timestamp().isBlank()) {
            try {
                locationTime = Instant.parse(request.timestamp());
            } catch (Exception ignored) {
                locationTime = Instant.now();
            }
        }
        status.setLastLocationUpdatedAt(locationTime);

        RiderStatus saved = riderStatusRepository.save(status);
        return RiderStatusResponse.fromEntity(saved);
    }

    private AppUser getRiderUser(String username) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el repartidor autenticado."));

        if (user.getRole() != UserRole.RIDER && user.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("El usuario autenticado no tiene rol de repartidor.");
        }

        return user;
    }
}
