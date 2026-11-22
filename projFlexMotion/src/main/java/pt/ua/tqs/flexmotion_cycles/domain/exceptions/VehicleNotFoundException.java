package pt.ua.tqs.flexmotion_cycles.domain.exceptions;

public class VehicleNotFoundException extends DomainException{
    private final Long vehicleId;

    public VehicleNotFoundException(Long vehicleId) {
        super("Vehicle not found: " + vehicleId);
        this.vehicleId = vehicleId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

}
