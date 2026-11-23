package pt.ua.tqs.flexmotion_cycles.domain;

/** Vehicle lifecycle states (see docs/bicycle_lifecycle.jpeg). REMOVED is final. */
public enum VehicleStatus {
    AVAILABLE,
    RESERVED,
    IN_USE,
    MAINTENANCE,
    REMOVED
}
