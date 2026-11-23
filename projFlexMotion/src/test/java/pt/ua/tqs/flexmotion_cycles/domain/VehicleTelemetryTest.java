package pt.ua.tqs.flexmotion_cycles.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

class VehicleTelemetryTest {

    private static final Instant FIRST = Instant.parse("2026-11-22T16:05:00Z");
    private static final Instant LATER = Instant.parse("2026-11-22T16:05:05Z");
    private static final GeoPoint POSITION = new GeoPoint(40.6405, -8.6538);

    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        vehicle = new Vehicle("EB-0001", VehicleType.E_BIKE, null);
    }

    @Test
    @Requirement("FM-26")
    void newVehicleIsLockedAndHasNoTelemetryYet() {
        assertThat(vehicle.isLocked()).isTrue();
        assertThat(vehicle.getPosition()).isNull();
        assertThat(vehicle.getLastTelemetryAt()).isNull();
    }

    @Test
    @Requirement("FM-26")
    void firstReadingUpdatesBatteryPositionAndLock() {
        boolean applied = vehicle.applyTelemetry(reading(FIRST, 19, false));

        assertThat(applied).isTrue();
        assertThat(vehicle.getBatteryLevel()).isEqualTo(19);
        assertThat(vehicle.getPosition()).isEqualTo(POSITION);
        assertThat(vehicle.isLocked()).isFalse();
        assertThat(vehicle.getLastTelemetryAt()).isEqualTo(FIRST);
    }

    @Test
    @Requirement("FM-26")
    void newerReadingReplacesPreviousOne() {
        vehicle.applyTelemetry(reading(FIRST, 80, true));

        boolean applied = vehicle.applyTelemetry(reading(LATER, 79, false));

        assertThat(applied).isTrue();
        assertThat(vehicle.getBatteryLevel()).isEqualTo(79);
        assertThat(vehicle.isLocked()).isFalse();
        assertThat(vehicle.getLastTelemetryAt()).isEqualTo(LATER);
    }

    @Test
    @Requirement("FM-26")
    void ignoresDuplicateReadingWithSameTimestamp() {
        vehicle.applyTelemetry(reading(FIRST, 80, true));

        boolean applied = vehicle.applyTelemetry(reading(FIRST, 10, false));

        assertThat(applied).isFalse();
        assertThat(vehicle.getBatteryLevel()).isEqualTo(80);
        assertThat(vehicle.isLocked()).isTrue();
    }

    @Test
    @Requirement("FM-26")
    void ignoresReadingOlderThanTheLastOne() {
        vehicle.applyTelemetry(reading(LATER, 79, false));

        boolean applied = vehicle.applyTelemetry(reading(FIRST, 80, true));

        assertThat(applied).isFalse();
        assertThat(vehicle.getBatteryLevel()).isEqualTo(79);
        assertThat(vehicle.getLastTelemetryAt()).isEqualTo(LATER);
    }

    private static TelemetryReading reading(Instant timestamp, int battery, boolean locked) {
        return new TelemetryReading("EB-0001", timestamp, battery, POSITION, locked, null);
    }
}
