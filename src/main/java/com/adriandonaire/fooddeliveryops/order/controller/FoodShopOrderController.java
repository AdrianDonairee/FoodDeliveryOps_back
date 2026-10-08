package com.adriandonaire.fooddeliveryops.order.controller;

import com.adriandonaire.fooddeliveryops.order.dto.CreateOrderRequest;
import com.adriandonaire.fooddeliveryops.order.dto.OrderResponse;
import com.adriandonaire.fooddeliveryops.order.service.OrderService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/food_shop/orders")
@PreAuthorize("hasAnyRole('FOOD_SHOP', 'ADMIN')")
public class FoodShopOrderController {

    private final OrderService orderService;

    public FoodShopOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request, Principal principal) {
        OrderResponse created = orderService.createOrder(request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(Principal principal) {
        List<OrderResponse> orders = orderService.getFoodShopOrders(principal.getName());
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id, Principal principal) {
        OrderResponse cancelled = orderService.cancelOrder(id, principal.getName());
        return ResponseEntity.ok(cancelled);
    }
}
