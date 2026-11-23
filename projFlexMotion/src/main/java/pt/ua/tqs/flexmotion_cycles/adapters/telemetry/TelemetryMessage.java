package pt.ua.tqs.flexmotion_cycles.adapters.telemetry;

import java.time.Instant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import pt.ua.tqs.flexmotion_cycles.domain.GeoPoint;
import pt.ua.tqs.flexmotion_cycles.domain.TelemetryReading;

record TelemetryMessage(
        @NotBlank String vehicleCode,
        @NotNull Instant timestamp,
        @NotNull @Min(MIN_BATTERY_PERCENT) @Max(MAX_BATTERY_PERCENT) Integer batteryPercent,
        @NotNull @Valid Position position,
        @NotNull Boolean locked,
        String faultEvent) {

    static final int MIN_BATTERY_PERCENT = 0;
    static final int MAX_BATTERY_PERCENT = 100;

    record Position(
            @NotNull @DecimalMin(MIN_LATITUDE) @DecimalMax(MAX_LATITUDE) Double latitude,
            @NotNull @DecimalMin(MIN_LONGITUDE) @DecimalMax(MAX_LONGITUDE) Double longitude) {

        static final String MIN_LATITUDE = "-90.0";
        static final String MAX_LATITUDE = "90.0";
        static final String MIN_LONGITUDE = "-180.0";
        static final String MAX_LONGITUDE = "180.0";
    }

    TelemetryReading toReading() {
        return new TelemetryReading(vehicleCode, timestamp, batteryPercent,
                new GeoPoint(position.latitude(), position.longitude()), locked, faultEvent);
    }
}
