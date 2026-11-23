package pt.ua.tqs.flexmotion_cycles.domain;

public enum VehicleType {
    E_BIKE(false),
    CARGO_E_BIKE(false),
    WHEELCHAIR_BIKE(true),
    HANDBIKE(true),
    ADAPTED_TRICYCLE(true);

    private final boolean accessible;

    VehicleType(boolean accessible) {
        this.accessible = accessible;
    }

    /** Accessible vehicles are only used through accessible bookings (BR19). */
    public boolean isAccessible() {
        return accessible;
    }
}
