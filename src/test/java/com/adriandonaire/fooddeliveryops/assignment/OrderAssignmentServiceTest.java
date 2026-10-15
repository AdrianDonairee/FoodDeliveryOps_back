package com.adriandonaire.fooddeliveryops.assignment;

import com.adriandonaire.fooddeliveryops.assignment.dto.DeliveryAssignmentResponse;
import com.adriandonaire.fooddeliveryops.assignment.repository.OrderAssignmentRepository;
import com.adriandonaire.fooddeliveryops.assignment.service.OrderAssignmentService;
import com.adriandonaire.fooddeliveryops.auth.AppUser;
import com.adriandonaire.fooddeliveryops.auth.UserRole;
import com.adriandonaire.fooddeliveryops.auth.repository.AppUserRepository;
import com.adriandonaire.fooddeliveryops.order.Order;
import com.adriandonaire.fooddeliveryops.order.OrderItem;
import com.adriandonaire.fooddeliveryops.order.OrderStatus;
import com.adriandonaire.fooddeliveryops.order.repository.OrderRepository;
import com.adriandonaire.fooddeliveryops.rider.AvailabilityStatus;
import com.adriandonaire.fooddeliveryops.rider.DutyStatus;
import com.adriandonaire.fooddeliveryops.rider.RiderStatus;
import com.adriandonaire.fooddeliveryops.rider.repository.RiderStatusRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OrderAssignmentServiceTest {

    @Autowired
    private OrderAssignmentService orderAssignmentService;

    @Autowired
    private OrderAssignmentRepository orderAssignmentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RiderStatusRepository riderStatusRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    private AppUser foodShop;
    private AppUser riderClose;
    private AppUser riderFar;

    @BeforeEach
    void setUp() {
        orderAssignmentRepository.deleteAll();
        orderRepository.deleteAll();
        riderStatusRepository.deleteAll();

        foodShop = appUserRepository.findByUsername("test_shop_assignment")
                .orElseGet(() -> appUserRepository.save(new AppUser("test_shop_assignment", "pass", UserRole.FOOD_SHOP)));

        riderClose = appUserRepository.findByUsername("rider_close")
                .orElseGet(() -> appUserRepository.save(new AppUser("rider_close", "pass", UserRole.RIDER)));

        riderFar = appUserRepository.findByUsername("rider_far")
                .orElseGet(() -> appUserRepository.save(new AppUser("rider_far", "pass", UserRole.RIDER)));

        // Setup riderClose: 1 km away, working, libre
        RiderStatus rsClose = new RiderStatus(riderClose);
        rsClose.setDutyStatus(DutyStatus.TRABAJANDO);
        rsClose.setAvailabilityStatus(AvailabilityStatus.LIBRE);
        rsClose.setCurrentLat(-34.6080);
        rsClose.setCurrentLng(-68.3301);
        rsClose.setLastLocationUpdatedAt(Instant.now());
        riderStatusRepository.save(rsClose);

        // Setup riderFar: 5 km away, working, libre
        RiderStatus rsFar = new RiderStatus(riderFar);
        rsFar.setDutyStatus(DutyStatus.TRABAJANDO);
        rsFar.setAvailabilityStatus(AvailabilityStatus.LIBRE);
        rsFar.setCurrentLat(-34.5700);
        rsFar.setCurrentLng(-68.3301);
        rsFar.setLastLocationUpdatedAt(Instant.now());
        riderStatusRepository.save(rsFar);
    }

    @Test
    @DisplayName("Asignación de pedido ofrece automáticamente al repartidor más cercano")
    void shouldAssignOrderToClosestEligibleRider() {
        Order order = createTestOrder();

        Optional<OrderAssignment> assignmentOpt = orderAssignmentService.assignOrder(order);

        assertThat(assignmentOpt).isPresent();
        OrderAssignment assignment = assignmentOpt.get();
        assertThat(assignment.getRider().getUsername()).isEqualTo("rider_close");
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.OFFERED);
        assertThat(assignment.getDistanceKm()).isLessThan(2.0);
    }

    @Test
    @DisplayName("Rechazo de oferta re-enruta secuencialmente al siguiente repartidor más cercano")
    void shouldRouteToNextClosestRiderWhenRejected() {
        Order order = createTestOrder();
        orderAssignmentService.assignOrder(order);

        // riderClose rejects
        orderAssignmentService.rejectAssignment(order.getId(), "rider_close");

        // Now riderFar should have the active offer
        DeliveryAssignmentResponse currentForFar = orderAssignmentService.getCurrentAssignment("rider_far");
        assertThat(currentForFar).isNotNull();
        assertThat(currentForFar.orderId()).isEqualTo(order.getId());

        // And riderClose should have no active offer
        DeliveryAssignmentResponse currentForClose = orderAssignmentService.getCurrentAssignment("rider_close");
        assertThat(currentForClose).isNull();
    }

    @Test
    @DisplayName("Aceptar oferta asigna el pedido y marca al repartidor como OCUPADO")
    void shouldAcceptAssignmentAndMarkRiderOccupied() {
        Order order = createTestOrder();
        orderAssignmentService.assignOrder(order);

        DeliveryAssignmentResponse response = orderAssignmentService.acceptAssignment(order.getId(), "rider_close");

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(OrderStatus.ASIGNADO);

        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.ASIGNADO);
        assertThat(updatedOrder.getAssignedRider().getUsername()).isEqualTo("rider_close");

        RiderStatus updatedRiderStatus = riderStatusRepository.findByRiderUsername("rider_close").orElseThrow();
        assertThat(updatedRiderStatus.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.OCUPADO);
    }

    @Test
    @DisplayName("Si todos los repartidores rechazan, el pedido permanece PENDIENTE sin error")
    void shouldRemainPendienteIfAllRidersReject() {
        Order order = createTestOrder();
        orderAssignmentService.assignOrder(order);

        orderAssignmentService.rejectAssignment(order.getId(), "rider_close");
        orderAssignmentService.rejectAssignment(order.getId(), "rider_far");

        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PENDIENTE);
        assertThat(updatedOrder.getAssignedRider()).isNull();
    }

    private Order createTestOrder() {
        Order order = new Order(
                foodShop,
                "Cliente Test",
                "2604112233",
                "Av. Libertador 500",
                "Dejar en puerta",
                -34.6150,
                -68.3300,
                5000.0
        );
        order.addItem(new OrderItem("Empanadas docena", 1, 5000.0));
        return orderRepository.save(order);
    }
}
