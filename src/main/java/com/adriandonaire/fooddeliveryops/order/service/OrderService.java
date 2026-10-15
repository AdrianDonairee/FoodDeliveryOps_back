package com.adriandonaire.fooddeliveryops.order.service;

import com.adriandonaire.fooddeliveryops.assignment.service.OrderAssignmentService;
import com.adriandonaire.fooddeliveryops.auth.AppUser;
import com.adriandonaire.fooddeliveryops.auth.repository.AppUserRepository;
import com.adriandonaire.fooddeliveryops.order.Order;
import com.adriandonaire.fooddeliveryops.order.OrderItem;
import com.adriandonaire.fooddeliveryops.order.OrderStatus;
import com.adriandonaire.fooddeliveryops.order.dto.CreateOrderRequest;
import com.adriandonaire.fooddeliveryops.order.dto.OrderResponse;
import com.adriandonaire.fooddeliveryops.order.repository.OrderRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final AppUserRepository appUserRepository;
    private final OrderAssignmentService orderAssignmentService;

    public OrderService(
            OrderRepository orderRepository,
            AppUserRepository appUserRepository,
            OrderAssignmentService orderAssignmentService
    ) {
        this.orderRepository = orderRepository;
        this.appUserRepository = appUserRepository;
        this.orderAssignmentService = orderAssignmentService;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String foodShopUsername) {
        AppUser foodShop = appUserRepository.findByUsername(foodShopUsername)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el comercio autenticado."));

        double calculatedTotal = request.items().stream()
                .mapToDouble(it -> it.quantity() * it.price())
                .sum();

        Order order = new Order(
                foodShop,
                request.customerName().trim(),
                request.customerPhone().trim(),
                request.customerAddress().trim(),
                request.customerNotes() != null ? request.customerNotes().trim() : null,
                request.customerLat(),
                request.customerLng(),
                calculatedTotal
        );

        request.items().forEach(it -> {
            OrderItem orderItem = new OrderItem(it.name().trim(), it.quantity(), it.price());
            order.addItem(orderItem);
        });

        Order saved = orderRepository.save(order);
        orderAssignmentService.assignOrder(saved);
        return OrderResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getFoodShopOrders(String foodShopUsername) {
        AppUser foodShop = appUserRepository.findByUsername(foodShopUsername)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el comercio autenticado."));

        return orderRepository.findByFoodShopIdWithItemsOrderByCreatedAtDesc(foodShop.getId()).stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, String foodShopUsername) {
        AppUser foodShop = appUserRepository.findByUsername(foodShopUsername)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el comercio autenticado."));

        Order order = orderRepository.findByIdAndFoodShopIdWithItems(orderId, foodShop.getId())
                .orElseThrow(() -> new IllegalArgumentException("El pedido #" + orderId + " no existe o no pertenece a tu comercio."));

        if (order.getStatus() != OrderStatus.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden cancelar pedidos en estado PENDIENTE. Estado actual: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELADO);
        Order updated = orderRepository.save(order);
        return OrderResponse.fromEntity(updated);
    }
}
