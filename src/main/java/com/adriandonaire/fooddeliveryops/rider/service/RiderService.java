package com.adriandonaire.fooddeliveryops.rider.service;

import com.adriandonaire.fooddeliveryops.assignment.dto.DeliveryAssignmentResponse;
import com.adriandonaire.fooddeliveryops.assignment.repository.OrderAssignmentRepository;
import com.adriandonaire.fooddeliveryops.auth.AppUser;
import com.adriandonaire.fooddeliveryops.auth.UserRole;
import com.adriandonaire.fooddeliveryops.auth.repository.AppUserRepository;
import com.adriandonaire.fooddeliveryops.order.Order;
import com.adriandonaire.fooddeliveryops.order.OrderStatus;
import com.adriandonaire.fooddeliveryops.order.dto.OrderResponse;
import com.adriandonaire.fooddeliveryops.order.repository.OrderRepository;
import com.adriandonaire.fooddeliveryops.rider.AvailabilityStatus;
import com.adriandonaire.fooddeliveryops.rider.DutyStatus;
import com.adriandonaire.fooddeliveryops.rider.RiderStatus;
import com.adriandonaire.fooddeliveryops.rider.dto.RiderProfileResponse;
import com.adriandonaire.fooddeliveryops.rider.dto.RiderStatusResponse;
import com.adriandonaire.fooddeliveryops.rider.dto.UpdateDeliveryStatusRequest;
import com.adriandonaire.fooddeliveryops.rider.dto.UpdateDutyStatusRequest;
import com.adriandonaire.fooddeliveryops.rider.dto.UpdateLocationRequest;
import com.adriandonaire.fooddeliveryops.rider.repository.RiderStatusRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiderService {

    private final RiderStatusRepository riderStatusRepository;
    private final AppUserRepository appUserRepository;
    private final OrderRepository orderRepository;
    private final OrderAssignmentRepository orderAssignmentRepository;

    public RiderService(
            RiderStatusRepository riderStatusRepository,
            AppUserRepository appUserRepository,
            OrderRepository orderRepository,
            OrderAssignmentRepository orderAssignmentRepository
    ) {
        this.riderStatusRepository = riderStatusRepository;
        this.appUserRepository = appUserRepository;
        this.orderRepository = orderRepository;
        this.orderAssignmentRepository = orderAssignmentRepository;
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

    /**
     * Advances the lifecycle state of an active delivery:
     * - ASIGNADO -> EN_CAMINO (food pickup)
     * - EN_CAMINO -> ENTREGADO (completed delivery)
     * Automatically sets availability to LIBRE on ENTREGADO.
     */
    @Transactional
    public RiderProfileResponse updateDeliveryStatus(Long orderId, UpdateDeliveryStatusRequest request, String username) {
        AppUser rider = getRiderUser(username);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el pedido #" + orderId));

        if (order.getAssignedRider() == null || !order.getAssignedRider().getId().equals(rider.getId())) {
            throw new IllegalArgumentException("Este pedido no te pertenece o no está asignado a tu usuario.");
        }

        OrderStatus currentStatus = order.getStatus();
        OrderStatus targetStatus = request.status();

        if (currentStatus == OrderStatus.ASIGNADO) {
            if (targetStatus != OrderStatus.EN_CAMINO) {
                throw new IllegalStateException("Transición inválida: un pedido en estado ASIGNADO solo puede avanzar a EN_CAMINO al retirar la comida.");
            }
        } else if (currentStatus == OrderStatus.EN_CAMINO) {
            if (targetStatus != OrderStatus.ENTREGADO) {
                throw new IllegalStateException("Transición inválida: un pedido en estado EN_CAMINO solo puede avanzar a ENTREGADO.");
            }
        } else if (currentStatus == OrderStatus.ENTREGADO || currentStatus == OrderStatus.CANCELADO) {
            throw new IllegalStateException("El pedido #" + orderId + " ya fue finalizado (" + currentStatus + ") y no admite modificaciones de estado.");
        } else {
            throw new IllegalStateException("No se puede avanzar el estado de un pedido en estado " + currentStatus + " desde este endpoint.");
        }

        order.setStatus(targetStatus);
        Order savedOrder = orderRepository.save(order);

        RiderStatus riderStatus = getOrCreateRiderStatusEntity(rider);

        if (targetStatus == OrderStatus.ENTREGADO) {
            riderStatus.setAvailabilityStatus(AvailabilityStatus.LIBRE);
            riderStatusRepository.save(riderStatus);
        }

        return buildProfileResponse(rider, riderStatus, targetStatus == OrderStatus.ENTREGADO ? null : savedOrder);
    }

    /**
     * Returns full operational rider profile, metrics and current active delivery.
     */
    @Transactional(readOnly = true)
    public RiderProfileResponse getRiderProfile(String username) {
        AppUser rider = getRiderUser(username);
        RiderStatus riderStatus = riderStatusRepository.findByRiderId(rider.getId())
                .orElseGet(() -> new RiderStatus(rider));

        Optional<Order> activeOrder = orderRepository.findActiveOrderByRiderId(
                rider.getId(),
                List.of(OrderStatus.ASIGNADO, OrderStatus.EN_CAMINO)
        );

        return buildProfileResponse(rider, riderStatus, activeOrder.orElse(null));
    }

    private RiderProfileResponse buildProfileResponse(AppUser rider, RiderStatus riderStatus, Order activeOrder) {
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();

        long deliveriesToday = orderRepository.countByRiderIdAndStatusAndUpdatedAtAfter(
                rider.getId(),
                OrderStatus.ENTREGADO,
                startOfDay
        );

        Double earningsToday = orderAssignmentRepository.sumEarningsByRiderIdAndSince(rider.getId(), startOfDay);
        if (earningsToday == null) {
            earningsToday = 0.0;
        }

        DeliveryAssignmentResponse activeDeliveryDto = null;
        Long orderId = null;
        OrderStatus orderStatus = null;
        OrderResponse orderResponse = null;

        if (activeOrder != null) {
            activeDeliveryDto = DeliveryAssignmentResponse.fromActiveOrder(activeOrder, 1800.0);
            orderId = activeOrder.getId();
            orderStatus = activeOrder.getStatus();
            orderResponse = OrderResponse.fromEntity(activeOrder);
        }

        String lastLoc = riderStatus.getLastLocationUpdatedAt() != null
                ? riderStatus.getLastLocationUpdatedAt().toString()
                : null;

        return new RiderProfileResponse(
                rider.getId(),
                rider.getUsername(),
                riderStatus.getDutyStatus(),
                riderStatus.getAvailabilityStatus(),
                riderStatus.getCurrentLat(),
                riderStatus.getCurrentLng(),
                lastLoc,
                deliveriesToday,
                earningsToday,
                activeDeliveryDto,
                orderId,
                orderStatus,
                orderResponse
        );
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
