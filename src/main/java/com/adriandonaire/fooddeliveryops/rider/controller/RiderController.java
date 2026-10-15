package com.adriandonaire.fooddeliveryops.rider.controller;

import com.adriandonaire.fooddeliveryops.rider.dto.RiderStatusResponse;
import com.adriandonaire.fooddeliveryops.rider.dto.UpdateDutyStatusRequest;
import com.adriandonaire.fooddeliveryops.rider.dto.UpdateLocationRequest;
import com.adriandonaire.fooddeliveryops.rider.service.RiderService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rider")
@PreAuthorize("hasAnyRole('RIDER', 'ADMIN')")
public class RiderController {

    private final RiderService riderService;

    public RiderController(RiderService riderService) {
        this.riderService = riderService;
    }

    @GetMapping("/status")
    public ResponseEntity<RiderStatusResponse> getStatus(Principal principal) {
        return ResponseEntity.ok(riderService.getRiderStatus(principal.getName()));
    }

    @PatchMapping("/status/duty")
    public ResponseEntity<RiderStatusResponse> updateDutyStatus(
            @Valid @RequestBody UpdateDutyStatusRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(riderService.updateDutyStatus(principal.getName(), request));
    }

    @PostMapping("/location")
    public ResponseEntity<RiderStatusResponse> updateLocation(
            @Valid @RequestBody UpdateLocationRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(riderService.updateLocation(principal.getName(), request));
    }
}
