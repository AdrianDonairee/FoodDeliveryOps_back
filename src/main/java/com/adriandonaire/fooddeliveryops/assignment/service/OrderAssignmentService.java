package com.adriandonaire.fooddeliveryops.assignment.service;

import com.adriandonaire.fooddeliveryops.assignment.AssignmentStatus;
import com.adriandonaire.fooddeliveryops.assignment.HaversineDistanceCalculator;
import com.adriandonaire.fooddeliveryops.assignment.OrderAssignment;
import com.adriandonaire.fooddeliveryops.assignment.dto.DeliveryAssignmentResponse;
import com.adriandonaire.fooddeliveryops.assignment.repository.OrderAssignmentRepository;
import com.adriandonaire.fooddeliveryops.auth.AppUser;
import com.adriandonaire.fooddeliveryops.auth.UserRole;
import com.adriandonaire.fooddeliveryops.auth.repository.AppUserRepository;
import com.adriandonaire.fooddeliveryops.order.Order;
import com.adriandonaire.fooddeliveryops.order.OrderStatus;
import com.adriandonaire.fooddeliveryops.order.repository.OrderRepository;
import com.adriandonaire.fooddeliveryops.rider.AvailabilityStatus;
import com.adriandonaire.fooddeliveryops.rider.DutyStatus;
import com.adriandonaire.fooddeliveryops.rider.RiderStatus;
import com.adriandonaire.fooddeliveryops.rider.repository.RiderStatusRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderAssignmentService {

    private final OrderAssignmentRepository orderAssignmentRepository;
    private final OrderRepository orderRepository;
    private final RiderStatusRepository riderStatusRepository;
    private final AppUserRepository appUserRepository;

    private final double maxSearchRadiusKm;
    private final int maxGpsAgeMinutes;
    private final int offerExpirationMinutes;
    private final double defaultShopLat;
    private final double defaultShopLng;

    public OrderAssignmentService(
            OrderAssignmentRepository orderAssignmentRepository,
            OrderRepository orderRepository,
            RiderStatusRepository riderStatusRepository,
            AppUserRepository appUserRepository,
            @Value("${app.assignment.max-search-radius-km:15.0}") double maxSearchRadiusKm,
            @Value("${app.assignment.max-gps-age-minutes:60}") int maxGpsAgeMinutes,
            @Value("${app.assignment.offer-expiration-minutes:5}") int offerExpirationMinutes,
            @Value("${app.assignment.default-shop-lat:-34.6177}") double defaultShopLat,
            @Value("${app.assignment.default-shop-lng:-68.3301}") double defaultShopLng
    ) {
        this.orderAssignmentRepository = orderAssignmentRepository;
        this.orderRepository = orderRepository;
        this.riderStatusRepository = riderStatusRepository;
        this.appUserRepository = appUserRepository;
        this.maxSearchRadiusKm = maxSearchRadiusKm;
        this.maxGpsAgeMinutes = maxGpsAgeMinutes;
        this.offerExpirationMinutes = offerExpirationMinutes;
        this.defaultShopLat = defaultShopLat;
        this.defaultShopLng = defaultShopLng;
    }

    /**
     * Attempts to find the closest eligible rider and routes an assignment offer.
     */
    @Transactional
    public Optional<OrderAssignment> assignOrder(Order order) {
        if (order.getStatus() != OrderStatus.PENDIENTE) {
            return Optional.empty();
        }

        // Cancel previous active offers for this order if any
        List<OrderAssignment> existingOffers = orderAssignmentRepository.findActiveOffersByOrderId(order.getId());
        for (OrderAssignment existing : existingOffers) {
            existing.setStatus(AssignmentStatus.CANCELLED);
            orderAssignmentRepository.save(existing);
        }

        List<Long> rejectedRiderIds = orderAssignmentRepository.findRejectedRiderIdsByOrderId(order.getId());
        Set<Long> excludedRiders = Set.copyOf(rejectedRiderIds);

        List<RiderStatus> eligibleRiders = riderStatusRepository.findByDutyStatusAndAvailabilityStatus(
                DutyStatus.TRABAJANDO,
                AvailabilityStatus.LIBRE
        );

        Instant gpsCutoff = Instant.now().minus(Duration.ofMinutes(maxGpsAgeMinutes));

        record Candidate(RiderStatus riderStatus, double distanceKm) {}

        double pickupLat = (order.getFoodShop() != null && order.getFoodShop().getLatitude() != null)
                ? order.getFoodShop().getLatitude()
                : defaultShopLat;
        double pickupLng = (order.getFoodShop() != null && order.getFoodShop().getLongitude() != null)
                ? order.getFoodShop().getLongitude()
                : defaultShopLng;

        List<Candidate> sortedCandidates = eligibleRiders.stream()
                .filter(rs -> rs.getCurrentLat() != null && rs.getCurrentLng() != null)
                .filter(rs -> rs.getLastLocationUpdatedAt() == null || rs.getLastLocationUpdatedAt().isAfter(gpsCutoff))
                .filter(rs -> !excludedRiders.contains(rs.getRider().getId()))
                .map(rs -> {
                    double dist = HaversineDistanceCalculator.calculateDistanceKm(
                            pickupLat,
                            pickupLng,
                            rs.getCurrentLat(),
                            rs.getCurrentLng()
                    );
                    return new Candidate(rs, dist);
                })
                .filter(candidate -> candidate.distanceKm() <= maxSearchRadiusKm)
                .sorted(Comparator.comparingDouble(Candidate::distanceKm))
                .toList();

        if (sortedCandidates.isEmpty()) {
            return Optional.empty();
        }

        Candidate closest = sortedCandidates.get(0);
        double calculatedEarnings = Math.max(1800.0, Math.round((1500.0 + closest.distanceKm() * 120.0) * 100.0) / 100.0);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(offerExpirationMinutes));

        OrderAssignment assignment = new OrderAssignment(
                order,
                closest.riderStatus().getRider(),
                closest.distanceKm(),
                calculatedEarnings,
                expiresAt
        );

        OrderAssignment saved = orderAssignmentRepository.save(assignment);
        return Optional.of(saved);
    }

    /**
     * Returns the rider's current active delivery or pending offer.
     */
    @Transactional(readOnly = true)
    public DeliveryAssignmentResponse getCurrentAssignment(String riderUsername) {
        AppUser rider = getRiderUser(riderUsername);

        // 1. Check if rider already has an active order (ASIGNADO or EN_CAMINO)
        Optional<Order> activeOrderOpt = orderRepository.findActiveOrderByRiderId(
                rider.getId(),
                List.of(OrderStatus.ASIGNADO, OrderStatus.EN_CAMINO)
        );

        if (activeOrderOpt.isPresent()) {
            Order active = activeOrderOpt.get();
            double earnings = 1800.0;
            return DeliveryAssignmentResponse.fromActiveOrder(active, earnings);
        }

        // 2. Check for active unexpired offer
        List<OrderAssignment> activeOffers = orderAssignmentRepository.findActiveOffersByRiderId(
                rider.getId(),
                AssignmentStatus.OFFERED,
                Instant.now()
        );

        if (!activeOffers.isEmpty()) {
            return DeliveryAssignmentResponse.fromOffer(activeOffers.get(0));
        }

        return null;
    }

    /**
     * Accepts a pending assignment offer.
     */
    @Transactional
    public DeliveryAssignmentResponse acceptAssignment(Long orderId, String riderUsername) {
        AppUser rider = getRiderUser(riderUsername);

        OrderAssignment assignment = orderAssignmentRepository.findByOrderIdAndRiderIdAndStatus(
                orderId,
                rider.getId(),
                AssignmentStatus.OFFERED
        ).orElseThrow(() -> new IllegalArgumentException("No hay una oferta activa para este pedido o la oferta ha expirado."));

        if (assignment.getExpiresAt().isBefore(Instant.now())) {
            assignment.setStatus(AssignmentStatus.EXPIRED);
            orderAssignmentRepository.save(assignment);
            throw new IllegalStateException("La oferta para este pedido ha expirado.");
        }

        Order order = assignment.getOrder();
        if (order.getStatus() != OrderStatus.PENDIENTE) {
            throw new IllegalStateException("El pedido #" + orderId + " ya no se encuentra disponible.");
        }

        // Accept offer
        assignment.setStatus(AssignmentStatus.ACCEPTED);
        assignment.setRespondedAt(Instant.now());
        orderAssignmentRepository.save(assignment);

        // Update Order
        order.setStatus(OrderStatus.ASIGNADO);
        order.setAssignedRider(rider);
        Order updatedOrder = orderRepository.save(order);

        // Update Rider status
        RiderStatus riderStatus = riderStatusRepository.findByRiderId(rider.getId())
                .orElseGet(() -> new RiderStatus(rider));
        riderStatus.setAvailabilityStatus(AvailabilityStatus.OCUPADO);
        riderStatusRepository.save(riderStatus);

        return DeliveryAssignmentResponse.fromActiveOrder(updatedOrder, assignment.getDeliveryEarnings());
    }

    /**
     * Rejects a pending assignment offer and routes the order to the next closest eligible rider.
     */
    @Transactional
    public void rejectAssignment(Long orderId, String riderUsername) {
        AppUser rider = getRiderUser(riderUsername);

        OrderAssignment assignment = orderAssignmentRepository.findByOrderIdAndRiderIdAndStatus(
                orderId,
                rider.getId(),
                AssignmentStatus.OFFERED
        ).orElseThrow(() -> new IllegalArgumentException("No se encontró una oferta activa para este pedido."));

        assignment.setStatus(AssignmentStatus.REJECTED);
        assignment.setRespondedAt(Instant.now());
        orderAssignmentRepository.save(assignment);

        Order order = assignment.getOrder();
        if (order.getStatus() == OrderStatus.PENDIENTE) {
            // Sequential re-assignment queue: search next closest rider excluding rejected
            assignOrder(order);
        }
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
