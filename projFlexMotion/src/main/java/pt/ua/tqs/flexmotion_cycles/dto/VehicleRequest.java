package pt.ua.tqs.flexmotion_cycles.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

/** Data the operator sends to register a vehicle at its current station. */
public record VehicleRequest(
        @NotBlank String code,
        @NotNull VehicleType type,
        @NotNull Long stationId) {
}