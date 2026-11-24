package pt.ua.tqs.fleet_simulator.domain;

/** Faults a vehicle can report (closed list of the telemetry contract). */
public enum FaultEvent {
    BRAKE_FAILURE,
    FLAT_TYRE,
    MOTOR_FAILURE
}