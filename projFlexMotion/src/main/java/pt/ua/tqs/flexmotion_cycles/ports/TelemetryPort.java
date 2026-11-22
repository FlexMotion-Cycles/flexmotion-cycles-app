package pt.ua.tqs.flexmotion_cycles.ports;

/**
 * Outbound port to the fleet. The answer to a command does not return here: it arrives
 * later as telemetry, published as an application event (e.g. {@link LockOpenedEvent}).
 */
public interface TelemetryPort {

    /** Asks the vehicle to release its lock. */
    void sendUnlockCommand(String vehicleCode);
}
