package pt.ua.tqs.flexmotion_cycles.domain;

import java.time.Instant;

public record TelemetryReading(
        String vehicleCode,
        Instant timestamp,
        int batteryPercent,
        GeoPoint position,
        boolean locked,
        String faultEvent) {
}
