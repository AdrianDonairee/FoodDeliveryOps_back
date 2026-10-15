package com.adriandonaire.fooddeliveryops.rider.dto;

import com.adriandonaire.fooddeliveryops.rider.DutyStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDutyStatusRequest(
        @NotNull(message = "El estado de turno (dutyStatus) es obligatorio.")
        DutyStatus dutyStatus
) {}
