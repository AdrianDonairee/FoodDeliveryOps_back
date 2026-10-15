package com.adriandonaire.fooddeliveryops.order;

import com.adriandonaire.fooddeliveryops.assignment.repository.OrderAssignmentRepository;
import com.adriandonaire.fooddeliveryops.auth.AppUser;
import com.adriandonaire.fooddeliveryops.auth.UserRole;
import com.adriandonaire.fooddeliveryops.auth.repository.AppUserRepository;
import com.adriandonaire.fooddeliveryops.order.repository.OrderRepository;
import com.adriandonaire.fooddeliveryops.rider.AvailabilityStatus;
import com.adriandonaire.fooddeliveryops.rider.DutyStatus;
import com.adriandonaire.fooddeliveryops.rider.RiderStatus;
import com.adriandonaire.fooddeliveryops.rider.dto.RiderProfileResponse;
import com.adriandonaire.fooddeliveryops.rider.dto.UpdateDeliveryStatusRequest;
import com.adriandonaire.fooddeliveryops.rider.repository.RiderStatusRepository;
import com.adriandonaire.fooddeliveryops.rider.service.RiderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OrderLifecycleStateMachineTest {

    @Autowired
    private RiderService riderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderAssignmentRepository orderAssignmentRepository;

    @Autowired
    private RiderStatusRepository riderStatusRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    private AppUser foodShop;
    private AppUser rider;
    private AppUser otherRider;

    @BeforeEach
    void setUp() {
        orderAssignmentRepository.deleteAll();
        orderRepository.deleteAll();
        riderStatusRepository.deleteAll();

        foodShop = appUserRepository.findByUsername("shop_lifecycle")
                .orElseGet(() -> appUserRepository.save(new AppUser("shop_lifecycle", "pass", UserRole.FOOD_SHOP)));

        rider = appUserRepository.findByUsername("rider_lifecycle")
                .orElseGet(() -> appUserRepository.save(new AppUser("rider_lifecycle", "pass", UserRole.RIDER)));

        otherRider = appUserRepository.findByUsername("rider_other")
                .orElseGet(() -> appUserRepository.save(new AppUser("rider_other", "pass", UserRole.RIDER)));

        RiderStatus rs = new RiderStatus(rider);
        rs.setDutyStatus(DutyStatus.TRABAJANDO);
        rs.setAvailabilityStatus(AvailabilityStatus.OCUPADO);
        riderStatusRepository.save(rs);
    }

    @Test
    @DisplayName("Transición exitosa: ASIGNADO -> EN_CAMINO")
    void shouldTransitionFromAsignadoToEnCamino() {
        Order order = createOrderWithStatus(OrderStatus.ASIGNADO, rider);

        RiderProfileResponse response = riderService.updateDeliveryStatus(
                order.getId(),
                new UpdateDeliveryStatusRequest(OrderStatus.EN_CAMINO),
                rider.getUsername()
        );

        assertThat(response).isNotNull();
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.EN_CAMINO);

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.EN_CAMINO);
    }

    @Test
    @DisplayName("Transición exitosa: EN_CAMINO -> ENTREGADO resetea disponibilidad a LIBRE")
    void shouldTransitionFromEnCaminoToEntregadoAndResetToLibre() {
        Order order = createOrderWithStatus(OrderStatus.EN_CAMINO, rider);

        RiderProfileResponse response = riderService.updateDeliveryStatus(
                order.getId(),
                new UpdateDeliveryStatusRequest(OrderStatus.ENTREGADO),
                rider.getUsername()
        );

        assertThat(response).isNotNull();
        assertThat(response.availabilityStatus()).isEqualTo(AvailabilityStatus.LIBRE);
        assertThat(response.activeDelivery()).isNull();

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.ENTREGADO);

        RiderStatus rs = riderStatusRepository.findByRiderUsername(rider.getUsername()).orElseThrow();
        assertThat(rs.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.LIBRE);
    }

    @Test
    @DisplayName("Transición ilegal: No se puede saltar directamente de ASIGNADO a ENTREGADO")
    void shouldRejectDirectTransitionFromAsignadoToEntregado() {
        Order order = createOrderWithStatus(OrderStatus.ASIGNADO, rider);

        assertThatThrownBy(() -> riderService.updateDeliveryStatus(
                order.getId(),
                new UpdateDeliveryStatusRequest(OrderStatus.ENTREGADO),
                rider.getUsername()
        )).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("solo puede avanzar a EN_CAMINO");
    }

    @Test
    @DisplayName("Transición ilegal: No se puede avanzar un pedido PENDIENTE sin asignación previa")
    void shouldRejectAdvanceOnPendienteOrder() {
        Order order = createOrderWithStatus(OrderStatus.PENDIENTE, null);

        assertThatThrownBy(() -> riderService.updateDeliveryStatus(
                order.getId(),
                new UpdateDeliveryStatusRequest(OrderStatus.EN_CAMINO),
                rider.getUsername()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Seguridad: Un repartidor no puede cambiar el estado del pedido de otro repartidor")
    void shouldRejectTransitionByUnauthorizedRider() {
        Order order = createOrderWithStatus(OrderStatus.ASIGNADO, otherRider);

        assertThatThrownBy(() -> riderService.updateDeliveryStatus(
                order.getId(),
                new UpdateDeliveryStatusRequest(OrderStatus.EN_CAMINO),
                rider.getUsername()
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("no te pertenece");
    }

    @Test
    @DisplayName("Terminal: No se puede modificar un pedido que ya está ENTREGADO")
    void shouldRejectModificationOnCompletedOrder() {
        Order order = createOrderWithStatus(OrderStatus.ENTREGADO, rider);

        assertThatThrownBy(() -> riderService.updateDeliveryStatus(
                order.getId(),
                new UpdateDeliveryStatusRequest(OrderStatus.EN_CAMINO),
                rider.getUsername()
        )).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("ya fue finalizado");
    }

    private Order createOrderWithStatus(OrderStatus status, AppUser assigned) {
        Order order = new Order(
                foodShop,
                "Cliente Test",
                "2604001122",
                "Calle Falsa 123",
                "Sin observaciones",
                -34.6177,
                -68.3301,
                4500.0
        );
        order.setStatus(status);
        order.setAssignedRider(assigned);
        order.addItem(new OrderItem("Lomito Completo", 1, 4500.0));
        return orderRepository.save(order);
    }
}
