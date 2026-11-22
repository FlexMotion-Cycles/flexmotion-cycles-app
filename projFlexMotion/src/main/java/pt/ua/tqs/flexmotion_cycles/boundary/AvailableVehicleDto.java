package pt.ua.tqs.flexmotion_cycles.boundary;

import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

public record AvailableVehicleDto(String code, VehicleType type, int batteryLevel) {

    static AvailableVehicleDto from(Vehicle vehicle) {
        return new AvailableVehicleDto(vehicle.getCode(), vehicle.getType(),
                vehicle.getBatteryLevel());
    }
}
