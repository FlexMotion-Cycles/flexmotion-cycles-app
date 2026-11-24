package pt.ua.tqs.flexmotion_cycles.ports;

import java.time.Instant;
import java.util.Objects;

import pt.ua.tqs.flexmotion_cycles.domain.GeoPoint;

public record LockClosedEvent(String vehicleCode, Instant closedAt, GeoPoint position) {
    public LockClosedEvent {
        Objects.requireNonNull(vehicleCode, "vehicleCode");
        Objects.requireNonNull(closedAt, "closedAt");
        Objects.requireNonNull(position, "position");
    }
}
