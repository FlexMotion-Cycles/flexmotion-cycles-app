package pt.ua.tqs.flexmotion_cycles.dto;

import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleState;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

/** Vehicle data returned by the API. */
public record VehicleResponse(
        Long id,
        String code,
        VehicleType type,
        VehicleState state,
        Long stationId) {

    /** Maps a vehicle to the data the API exposes; stationId is null while it is away. */
    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getCode(),
                vehicle.getType(),
                vehicle.getState(),
                vehicle.getStation() == null ? null : vehicle.getStation().getId());
    }
}