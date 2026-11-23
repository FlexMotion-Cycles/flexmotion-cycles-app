package pt.ua.tqs.flexmotion_cycles.boundary;

import jakarta.validation.constraints.NotBlank;

public record UnlockRequest(@NotBlank String vehicleCode) {
}
