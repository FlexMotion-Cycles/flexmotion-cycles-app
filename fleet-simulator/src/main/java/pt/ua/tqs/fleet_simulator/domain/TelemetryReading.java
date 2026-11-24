package pt.ua.tqs.fleet_simulator.domain;

import java.time.Instant;

/** What one vehicle reports at one moment. {@code faultEvent} is null when there is no new fault. */
public record TelemetryReading(
        String vehicleCode,
        Instant timestamp,
        int batteryPercent,
        GeoPoint position,
        boolean locked,
        FaultEvent faultEvent) {
}