package com.adriandonaire.fooddeliveryops.assignment;

import com.adriandonaire.fooddeliveryops.assignment.controller.RiderAssignmentController;
import com.adriandonaire.fooddeliveryops.assignment.dto.DeliveryAssignmentResponse;
import com.adriandonaire.fooddeliveryops.assignment.dto.DeliveryOfferItemResponse;
import com.adriandonaire.fooddeliveryops.assignment.service.OrderAssignmentService;
import com.adriandonaire.fooddeliveryops.common.GlobalExceptionHandler;
import com.adriandonaire.fooddeliveryops.order.OrderStatus;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RiderAssignmentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderAssignmentService orderAssignmentService;

    @InjectMocks
    private RiderAssignmentController riderAssignmentController;

    private final Principal riderPrincipal = () -> "rider_test";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(riderAssignmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /rider/assignments/current retorna 204 No Content si no hay asignaciones")
    void getCurrentAssignmentReturns204WhenNoOffers() throws Exception {
        when(orderAssignmentService.getCurrentAssignment("rider_test")).thenReturn(null);

        mockMvc.perform(get("/rider/assignments/current")
                        .principal(riderPrincipal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /rider/assignments/current retorna 200 con la oferta activa")
    void getCurrentAssignmentReturns200WithOffer() throws Exception {
        DeliveryAssignmentResponse mockOffer = new DeliveryAssignmentResponse(
                10L,
                OrderStatus.PENDIENTE,
                "Pizzeria San Rafael",
                "Av. Mitre 340",
                "Juan Perez",
                "2604998877",
                "San Martin 123",
                "Tocar timbre",
                -34.6100,
                -68.3300,
                List.of(new DeliveryOfferItemResponse("Pizza", 1, 8000.0)),
                8000.0,
                1800.0,
                "2026-10-15T22:00:00Z",
                null
        );

        when(orderAssignmentService.getCurrentAssignment("rider_test")).thenReturn(mockOffer);

        mockMvc.perform(get("/rider/assignments/current")
                        .principal(riderPrincipal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(10))
                .andExpect(jsonPath("$.foodShopName").value("Pizzeria San Rafael"))
                .andExpect(jsonPath("$.deliveryEarnings").value(1800.0));
    }

    @Test
    @DisplayName("POST /rider/assignments/{orderId}/accept retorna 200 con la asignación aceptada")
    void acceptAssignmentReturns200() throws Exception {
        DeliveryAssignmentResponse mockAccepted = new DeliveryAssignmentResponse(
                10L,
                OrderStatus.ASIGNADO,
                "Pizzeria San Rafael",
                "Av. Mitre 340",
                "Juan Perez",
                "2604998877",
                "San Martin 123",
                "Tocar timbre",
                -34.6100,
                -68.3300,
                List.of(new DeliveryOfferItemResponse("Pizza", 1, 8000.0)),
                8000.0,
                1800.0,
                null,
                "2026-10-15T21:55:00Z"
        );

        when(orderAssignmentService.acceptAssignment(eq(10L), eq("rider_test"))).thenReturn(mockAccepted);

        mockMvc.perform(post("/rider/assignments/10/accept")
                        .principal(riderPrincipal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(10))
                .andExpect(jsonPath("$.status").value("ASIGNADO"));
    }

    @Test
    @DisplayName("POST /rider/assignments/{orderId}/reject retorna 200 con confirmación de rechazo")
    void rejectAssignmentReturns200() throws Exception {
        doNothing().when(orderAssignmentService).rejectAssignment(eq(10L), eq("rider_test"));

        mockMvc.perform(post("/rider/assignments/10/reject")
                        .principal(riderPrincipal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Oferta de pedido rechazada correctamente."));
    }
}
