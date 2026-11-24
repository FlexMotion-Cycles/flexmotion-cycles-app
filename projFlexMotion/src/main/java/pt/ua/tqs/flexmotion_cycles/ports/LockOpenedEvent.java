package pt.ua.tqs.flexmotion_cycles.ports;

import java.time.Instant;

/** Telemetry: the lock of a vehicle opened at the given time (trip start, BR7). */
public record LockOpenedEvent(String vehicleCode, Instant openedAt) {
}
