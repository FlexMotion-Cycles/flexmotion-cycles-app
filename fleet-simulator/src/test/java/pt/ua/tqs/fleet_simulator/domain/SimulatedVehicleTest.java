package pt.ua.tqs.fleet_simulator.domain;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

class SimulatedVehicleTest {

    private static final GeoPoint CENTRAL_SQUARE = new GeoPoint(40.6405, -8.6538);
    private static final GeoPoint RIVERSIDE = new GeoPoint(40.6420, -8.6500);
    private static final Instant NOW = Instant.parse("2026-11-23T10:15:30Z");

    @Test
    @Requirement("FM-25")
    void startsLockedWithItsInitialBatteryAndPosition() {
        SimulatedVehicle vehicle = new SimulatedVehicle("EB-0001", 80, CENTRAL_SQUARE);

        assertThat(vehicle.takeReading(NOW)).isEqualTo(
                new TelemetryReading("EB-0001", NOW, 80, CENTRAL_SQUARE, true, null));
    }

    @Test
    @Requirement("FM-25")
    void refusesBatteryBelowZeroPercent() {
        assertThatThrownBy(() -> new SimulatedVehicle("EB-0001", -1, CENTRAL_SQUARE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 100})
    @Requirement("FM-25")
    void acceptsBatteryAtTheLimits(int batteryPercent) {
        SimulatedVehicle vehicle = new SimulatedVehicle("EB-0001", batteryPercent, CENTRAL_SQUARE);

        assertThat(vehicle.takeReading(NOW).batteryPercent()).isEqualTo(batteryPercent);
    }

    @Test
    @Requirement("FM-25")
    void refusesBatteryAboveHundredPercent() {
        assertThatThrownBy(() -> new SimulatedVehicle("EB-0001", 101, CENTRAL_SQUARE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "EB/0001", "EB+0001", "EB#0001"})
    @Requirement("FM-25")
    void refusesCodeThatCannotBeAnMqttTopicLevel(String code) {
        assertThatThrownBy(() -> new SimulatedVehicle(code, 80, CENTRAL_SQUARE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Requirement("FM-25")
    void unlockOpensTheLock() {
        SimulatedVehicle vehicle = new SimulatedVehicle("EB-0001", 80, CENTRAL_SQUARE);

        vehicle.unlock();

        assertThat(vehicle.takeReading(NOW).locked()).isFalse();
    }

    @Test
    @Requirement("FM-25")
    void lockedVehicleKeepsItsBattery() {
        SimulatedVehicle vehicle = new SimulatedVehicle("EB-0001", 80, CENTRAL_SQUARE);

        vehicle.drain(1);

        assertThat(vehicle.takeReading(NOW).batteryPercent()).isEqualTo(80);
    }

    @Test
    @Requirement("FM-25")
    void batteryDrainsWhileTheVehicleIsInUse() {
        SimulatedVehicle vehicle = new SimulatedVehicle("EB-0001", 80, CENTRAL_SQUARE);
        vehicle.unlock();

        vehicle.drain(1);

        assertThat(vehicle.takeReading(NOW).batteryPercent()).isEqualTo(79);
    }

    @Test
    @Requirement("FM-25")
    void batteryAtOnePercentDrainsToZero() {
        SimulatedVehicle vehicle = new SimulatedVehicle("EB-0001", 1, CENTRAL_SQUARE);
        vehicle.unlock();

        vehicle.drain(1);

        assertThat(vehicle.takeReading(NOW).batteryPercent()).isZero();
    }

    @Test
    @Requirement("FM-25")
    void batteryNeverDropsBelowZero() {
        SimulatedVehicle vehicle = new SimulatedVehicle("EB-0001", 0, CENTRAL_SQUARE);
        vehicle.unlock();

        vehicle.drain(1);

        assertThat(vehicle.takeReading(NOW).batteryPercent()).isZero();
    }

    @Test
    @Requirement("FM-25")
    void lockClosesTheLockWhereTheVehicleIs() {
        SimulatedVehicle vehicle = new SimulatedVehicle("EB-0001", 80, CENTRAL_SQUARE);
        vehicle.unlock();

        vehicle.lock();

        TelemetryReading reading = vehicle.takeReading(NOW);
        assertThat(reading.locked()).isTrue();
        assertThat(reading.position()).isEqualTo(CENTRAL_SQUARE);
    }

    @Test
    @Requirement("FM-25")
    void lockAtDocksTheVehicleAtTheGivenPosition() {
        SimulatedVehicle vehicle = new SimulatedVehicle("EB-0001", 80, CENTRAL_SQUARE);
        vehicle.unlock();

        vehicle.lockAt(RIVERSIDE);

        TelemetryReading reading = vehicle.takeReading(NOW);
        assertThat(reading.locked()).isTrue();
        assertThat(reading.position()).isEqualTo(RIVERSIDE);
    }

    @Test
    @Requirement("FM-25")
    void faultIsReportedOnlyInTheNextReading() {
        SimulatedVehicle vehicle = new SimulatedVehicle("EB-0001", 80, CENTRAL_SQUARE);

        vehicle.triggerFault(FaultEvent.BRAKE_FAILURE);

        assertThat(vehicle.takeReading(NOW).faultEvent()).isEqualTo(FaultEvent.BRAKE_FAILURE);
        assertThat(vehicle.takeReading(NOW).faultEvent()).isNull();
    }
}