package com.adriandonaire.fooddeliveryops;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
            "status", "success",
            "message", "FoodDeliveryOps API running on Azure Container Apps",
            "timestamp", Instant.now().toString()
        );
    }

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of("greating", "Hello World");
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "healthy",
            "timestamp", Instant.now().toString(),
            "service", "FoodDeliveryOps-api"
        );
    }
}