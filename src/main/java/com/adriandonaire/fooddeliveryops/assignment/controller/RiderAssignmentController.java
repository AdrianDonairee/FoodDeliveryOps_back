package com.adriandonaire.fooddeliveryops.assignment.controller;

import com.adriandonaire.fooddeliveryops.assignment.dto.DeliveryAssignmentResponse;
import com.adriandonaire.fooddeliveryops.assignment.service.OrderAssignmentService;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rider/assignments")
@PreAuthorize("hasAnyRole('RIDER', 'ADMIN')")
public class RiderAssignmentController {

    private final OrderAssignmentService orderAssignmentService;

    public RiderAssignmentController(OrderAssignmentService orderAssignmentService) {
        this.orderAssignmentService = orderAssignmentService;
    }

    @GetMapping("/current")
    public ResponseEntity<DeliveryAssignmentResponse> getCurrentAssignment(Principal principal) {
        DeliveryAssignmentResponse current = orderAssignmentService.getCurrentAssignment(principal.getName());
        if (current == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(current);
    }

    @PostMapping("/{orderId}/accept")
    public ResponseEntity<DeliveryAssignmentResponse> acceptAssignment(
            @PathVariable Long orderId,
            Principal principal
    ) {
        DeliveryAssignmentResponse accepted = orderAssignmentService.acceptAssignment(orderId, principal.getName());
        return ResponseEntity.ok(accepted);
    }

    @PostMapping("/{orderId}/reject")
    public ResponseEntity<Map<String, String>> rejectAssignment(
            @PathVariable Long orderId,
            Principal principal
    ) {
        orderAssignmentService.rejectAssignment(orderId, principal.getName());
        return ResponseEntity.ok(Map.of("message", "Oferta de pedido rechazada correctamente."));
    }
}
