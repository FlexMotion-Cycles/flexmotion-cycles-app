package pt.ua.tqs.flexmotion_cycles.boundary;

import jakarta.validation.constraints.NotBlank;

public record ReservationRequest(@NotBlank String vehicleCode) {
}
