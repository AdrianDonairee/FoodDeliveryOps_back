package com.adriandonaire.fooddeliveryops.order;

import com.adriandonaire.fooddeliveryops.auth.AppUser;
import com.adriandonaire.fooddeliveryops.auth.UserRole;
import com.adriandonaire.fooddeliveryops.auth.repository.AppUserRepository;
import com.adriandonaire.fooddeliveryops.common.GlobalExceptionHandler;
import com.adriandonaire.fooddeliveryops.order.controller.FoodShopOrderController;
import com.adriandonaire.fooddeliveryops.order.dto.CreateOrderItemRequest;
import com.adriandonaire.fooddeliveryops.order.dto.CreateOrderRequest;
import com.adriandonaire.fooddeliveryops.order.repository.OrderRepository;
import com.adriandonaire.fooddeliveryops.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class FoodShopOrderControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private OrderService orderService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final Principal foodShopPrincipal = () -> "test_food_shop";

    @BeforeEach
    void setUp() {
        FoodShopOrderController controller = new FoodShopOrderController(orderService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        orderRepository.deleteAll();

        String username = "test_food_shop";
        appUserRepository.findByUsername(username).orElseGet(() -> {
            AppUser user = new AppUser(username, passwordEncoder.encode("password123"), UserRole.FOOD_SHOP);
            return appUserRepository.save(user);
        });
    }

    @Test
    @DisplayName("Debe crear un pedido, listarlo y luego cancelarlo exitosamente")
    void testCreateListAndCancelOrderFlow() throws Exception {
        // 1. Crear pedido
        CreateOrderRequest request = new CreateOrderRequest(
                "Lucas Pérez",
                "260-4123456",
                "San Luis 250, San Rafael",
                "Portón gris, timbre 2",
                -34.6177,
                -68.3301,
                List.of(
                        new CreateOrderItemRequest("Pizza Muzzarella", 1, 8500.0),
                        new CreateOrderItemRequest("Empanadas Carne", 6, 1000.0)
                )
        );

        MvcResult createResult = mockMvc.perform(post("/food_shop/orders")
                        .principal(foodShopPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.customerName", is("Lucas Pérez")))
                .andExpect(jsonPath("$.customerAddress", is("San Luis 250, San Rafael")))
                .andExpect(jsonPath("$.customerNotes", is("Portón gris, timbre 2")))
                .andExpect(jsonPath("$.totalPrice", is(14500.0)))
                .andExpect(jsonPath("$.status", is("PENDIENTE")))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andReturn();

        String responseJson = createResult.getResponse().getContentAsString();
        Number orderIdNum = com.jayway.jsonpath.JsonPath.read(responseJson, "$.id");
        Long orderId = orderIdNum.longValue();

        // 2. Listar pedidos del comercio
        mockMvc.perform(get("/food_shop/orders")
                        .principal(foodShopPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].id", is(orderId.intValue())))
                .andExpect(jsonPath("$[0].customerName", is("Lucas Pérez")));

        // 3. Cancelar pedido en estado PENDIENTE
        mockMvc.perform(post("/food_shop/orders/" + orderId + "/cancel")
                        .principal(foodShopPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(orderId.intValue())))
                .andExpect(jsonPath("$.status", is("CANCELADO")));

        // 4. Intentar cancelar nuevamente (debe dar 400 Bad Request)
        mockMvc.perform(post("/food_shop/orders/" + orderId + "/cancel")
                        .principal(foodShopPrincipal))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    @DisplayName("Debe rechazar la creación de un pedido con datos inválidos o incompletos")
    void testCreateOrderValidationFailure() throws Exception {
        CreateOrderRequest invalidRequest = new CreateOrderRequest(
                "", // nombre vacío
                "",
                "",
                null,
                null,
                null,
                List.of() // sin ítems
        );

        mockMvc.perform(post("/food_shop/orders")
                        .principal(foodShopPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", notNullValue()));
    }
}
