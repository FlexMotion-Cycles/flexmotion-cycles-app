package pt.ua.tqs.flexmotion_cycles.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pt.ua.tqs.flexmotion_cycles.domain.StationType;

/**
 * Data the operator sends to register a station. Step-free access and technician hours
 * only apply to accessible hubs.
 */
public record StationRequest(
        @NotBlank String name,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotNull StationType type,
        @NotNull Integer dockCapacity,
        Boolean stepFreeAccess,
        LocalTime technicianFrom,
        LocalTime technicianTo) {
}