package com.adriandonaire.fooddeliveryops.auth.service;

import com.adriandonaire.fooddeliveryops.auth.AppUser;
import com.adriandonaire.fooddeliveryops.auth.RefreshToken;
import com.adriandonaire.fooddeliveryops.auth.UserRole;
import com.adriandonaire.fooddeliveryops.auth.dto.AccountResponse;
import com.adriandonaire.fooddeliveryops.auth.dto.AuthResponse;
import com.adriandonaire.fooddeliveryops.auth.dto.CreateUserRequest;
import com.adriandonaire.fooddeliveryops.auth.dto.LoginRequest;
import com.adriandonaire.fooddeliveryops.auth.dto.RefreshRequest;
import com.adriandonaire.fooddeliveryops.auth.dto.UpdateUserRequest;
import com.adriandonaire.fooddeliveryops.auth.dto.UserResponse;
import com.adriandonaire.fooddeliveryops.assignment.repository.OrderAssignmentRepository;
import com.adriandonaire.fooddeliveryops.auth.repository.AppUserRepository;
import com.adriandonaire.fooddeliveryops.auth.repository.RefreshTokenRepository;
import com.adriandonaire.fooddeliveryops.order.repository.OrderRepository;
import com.adriandonaire.fooddeliveryops.rider.repository.RiderStatusRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RiderStatusRepository riderStatusRepository;
    private final OrderAssignmentRepository orderAssignmentRepository;
    private final OrderRepository orderRepository;

    public AuthService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            RefreshTokenRepository refreshTokenRepository,
            RiderStatusRepository riderStatusRepository,
            OrderAssignmentRepository orderAssignmentRepository,
            OrderRepository orderRepository) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.riderStatusRepository = riderStatusRepository;
        this.orderAssignmentRepository = orderAssignmentRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByUsername(normalizeUsername(request.username()))
                .orElseThrow(() -> new IllegalArgumentException("Nombre de usuario o contraseña inválidos."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Nombre de usuario o contraseña inválidos.");
        }

        return issueSession(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenService.requireValidToken(request.refreshToken());
        RefreshToken rotated = refreshTokenService.rotateToken(refreshToken);
        return buildResponse(rotated.getUser(), rotated.getToken());
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenService.revokeToken(request.refreshToken());
    }

    @Transactional
    public AccountResponse createUser(CreateUserRequest request) {
        String username = normalizeUsername(request.username());

        if (username.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }

        if (appUserRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Ese nombre de usuario ya existe.");
        }

        if (request.role() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Las cuentas admin solo se crean por semilla o migración.");
        }

        AppUser newUser = new AppUser(username, passwordEncoder.encode(request.password()), request.role());
        AppUser savedUser = appUserRepository.save(newUser);
        return new AccountResponse(savedUser.getId(), savedUser.getUsername(), savedUser.getRole());
    }

    @Transactional
    public List<AccountResponse> listUsers() {
        return appUserRepository.findAll().stream()
                .map(user -> new AccountResponse(user.getId(), user.getUsername(), user.getRole()))
                .toList();
    }

    @Transactional
    public AccountResponse updateUser(Long id, UpdateUserRequest request) {
        AppUser existingUser = appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La cuenta no existe."));

        if (existingUser.getRole() == UserRole.ADMIN || request.role() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Las cuentas admin no se pueden editar desde este panel.");
        }

        String username = normalizeUsername(request.username());
        if (username.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }

        if (appUserRepository.existsByUsernameAndIdNot(username, id)) {
            throw new IllegalArgumentException("Ese nombre de usuario ya existe.");
        }

        existingUser.setUsername(username);
        existingUser.setRole(request.role());

        if (request.password() != null && !request.password().trim().isEmpty()) {
            existingUser.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        AppUser savedUser = appUserRepository.save(existingUser);
        return new AccountResponse(savedUser.getId(), savedUser.getUsername(), savedUser.getRole());
    }

    @Transactional
    public void deleteUser(Long id) {
        AppUser existingUser = appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La cuenta no existe."));

        if (existingUser.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Las cuentas admin no se pueden eliminar desde este panel.");
        }

        if (orderRepository.existsByFoodShopId(id) || orderRepository.existsByAssignedRiderId(id)) {
            throw new IllegalArgumentException("No se puede eliminar la cuenta porque posee pedidos registrados en el sistema.");
        }

        orderAssignmentRepository.deleteAllByRiderId(id);
        riderStatusRepository.deleteByRiderId(id);
        refreshTokenRepository.deleteAllByUser(existingUser);
        appUserRepository.delete(existingUser);
    }

    private AuthResponse issueSession(AppUser user) {
        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.issueToken(user);
        return buildResponse(user, accessToken, refreshToken.getToken());
    }

    private AuthResponse buildResponse(AppUser user, String refreshTokenValue) {
        String accessToken = jwtService.generateAccessToken(user);
        return buildResponse(user, accessToken, refreshTokenValue);
    }

    private AuthResponse buildResponse(AppUser user, String accessToken, String refreshTokenValue) {
        return new AuthResponse(accessToken, refreshTokenValue, "Bearer", new UserResponse(user.getUsername(), user.getRole()));
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase();
    }
}