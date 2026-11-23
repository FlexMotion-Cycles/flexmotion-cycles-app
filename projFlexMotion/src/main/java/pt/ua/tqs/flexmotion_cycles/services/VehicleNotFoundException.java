package pt.ua.tqs.flexmotion_cycles.services;

public class VehicleNotFoundException extends RuntimeException {

    public VehicleNotFoundException(String vehicleCode) {
        super("Vehicle " + vehicleCode + " not found");
    }
}
