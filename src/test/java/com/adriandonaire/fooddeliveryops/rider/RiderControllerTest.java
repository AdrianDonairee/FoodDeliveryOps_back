package com.adriandonaire.fooddeliveryops.rider;

import com.adriandonaire.fooddeliveryops.auth.AppUser;
import com.adriandonaire.fooddeliveryops.auth.UserRole;
import com.adriandonaire.fooddeliveryops.auth.repository.AppUserRepository;
import com.adriandonaire.fooddeliveryops.common.GlobalExceptionHandler;
import com.adriandonaire.fooddeliveryops.rider.controller.RiderController;
import com.adriandonaire.fooddeliveryops.rider.dto.UpdateDutyStatusRequest;
import com.adriandonaire.fooddeliveryops.rider.dto.UpdateLocationRequest;
import com.adriandonaire.fooddeliveryops.rider.repository.RiderStatusRepository;
import com.adriandonaire.fooddeliveryops.rider.service.RiderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class RiderControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RiderService riderService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private RiderStatusRepository riderStatusRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String riderUsername = "test_rider_moto";
    private final Principal riderPrincipal = () -> riderUsername;
    private AppUser riderUser;

    @BeforeEach
    void setUp() {
        RiderController controller = new RiderController(riderService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        riderStatusRepository.deleteAll();

        riderUser = appUserRepository.findByUsername(riderUsername).orElseGet(() -> {
            AppUser user = new AppUser(riderUsername, passwordEncoder.encode("rider123"), UserRole.RIDER);
            return appUserRepository.save(user);
        });
    }

    @Test
    @DisplayName("Debe consultar el estado actual del repartidor con valores por defecto")
    void testGetRiderStatusDefault() throws Exception {
        mockMvc.perform(get("/rider/status")
                        .principal(riderPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riderUsername", is(riderUsername)))
                .andExpect(jsonPath("$.dutyStatus", is("NO_TRABAJANDO")))
                .andExpect(jsonPath("$.availabilityStatus", is("LIBRE")));
    }

    @Test
    @DisplayName("Debe permitir al repartidor activar su turno laboral (TRABAJANDO)")
    void testUpdateDutyStatusToTrabajando() throws Exception {
        UpdateDutyStatusRequest request = new UpdateDutyStatusRequest(DutyStatus.TRABAJANDO);

        mockMvc.perform(patch("/rider/status/duty")
                        .principal(riderPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dutyStatus", is("TRABAJANDO")))
                .andExpect(jsonPath("$.availabilityStatus", is("LIBRE")));
    }

    @Test
    @DisplayName("Debe registrar la ubicación GPS cuando el repartidor está TRABAJANDO")
    void testUpdateLocationWhenTrabajando() throws Exception {
        // 1. Activar turno
        riderService.updateDutyStatus(riderUsername, new UpdateDutyStatusRequest(DutyStatus.TRABAJANDO));

        // 2. Enviar coordenadas GPS
        UpdateLocationRequest locationRequest = new UpdateLocationRequest(-34.6177, -68.3301, Instant.now().toString());

        mockMvc.perform(post("/rider/location")
                        .principal(riderPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(locationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dutyStatus", is("TRABAJANDO")))
                .andExpect(jsonPath("$.currentLat", is(-34.6177)))
                .andExpect(jsonPath("$.currentLng", is(-68.3301)))
                .andExpect(jsonPath("$.lastLocationUpdatedAt", notNullValue()));
    }

    @Test
    @DisplayName("Debe rechazar la ingesta de ubicación GPS si el repartidor NO está TRABAJANDO")
    void testUpdateLocationWhenNoTrabajandoFails() throws Exception {
        UpdateLocationRequest locationRequest = new UpdateLocationRequest(-34.6177, -68.3301, Instant.now().toString());

        mockMvc.perform(post("/rider/location")
                        .principal(riderPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(locationRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("El repartidor debe estar en estado TRABAJANDO para enviar su ubicación GPS.")));
    }

    @Test
    @DisplayName("Debe validar que las coordenadas GPS estén dentro de los límites terrestres válidos")
    void testUpdateLocationInvalidCoordinatesValidation() throws Exception {
        // Latitud inválida: 150.0 (máximo es 90.0)
        UpdateLocationRequest invalidRequest = new UpdateLocationRequest(150.0, -68.3301, Instant.now().toString());

        mockMvc.perform(post("/rider/location")
                        .principal(riderPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    @DisplayName("No debe permitir salir de turno (NO_TRABAJANDO) si el repartidor está OCUPADO con un pedido")
    void testCannotSwitchToNoTrabajandoWhileOcupado() throws Exception {
        // Poner en TRABAJANDO y simular OCUPADO
        RiderStatus statusEntity = riderService.getOrCreateRiderStatusEntity(riderUser);
        statusEntity.setDutyStatus(DutyStatus.TRABAJANDO);
        statusEntity.setAvailabilityStatus(AvailabilityStatus.OCUPADO);
        riderStatusRepository.save(statusEntity);

        UpdateDutyStatusRequest request = new UpdateDutyStatusRequest(DutyStatus.NO_TRABAJANDO);

        mockMvc.perform(patch("/rider/status/duty")
                        .principal(riderPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("No puedes cambiar a NO_TRABAJANDO mientras tengas un pedido asignado o en curso.")));
    }
}
