package com.adriandonaire.fooddeliveryops.auth.controller;

import com.adriandonaire.fooddeliveryops.auth.dto.CreateUserRequest;
import com.adriandonaire.fooddeliveryops.auth.dto.AccountResponse;
import com.adriandonaire.fooddeliveryops.auth.dto.UpdateUserRequest;
import com.adriandonaire.fooddeliveryops.auth.service.AuthService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AuthService authService;

    public AdminUserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> listUsers() {
        return ResponseEntity.ok(authService.listUsers());
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(authService.createUser(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> updateUser(@RequestBody UpdateUserRequest request, @org.springframework.web.bind.annotation.PathVariable Long id) {
        return ResponseEntity.ok(authService.updateUser(id, request));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@org.springframework.web.bind.annotation.PathVariable Long id) {
        authService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}