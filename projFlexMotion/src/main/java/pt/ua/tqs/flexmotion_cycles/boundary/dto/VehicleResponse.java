package pt.ua.tqs.flexmotion_cycles.boundary.dto;

import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleState;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

public record VehicleResponse(Long id, String code, VehicleType type, VehicleState state) {
    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(vehicle.getId(), vehicle.getCode(), vehicle.getType(), vehicle.getState());
    }
}
