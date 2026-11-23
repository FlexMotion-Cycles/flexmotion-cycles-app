package pt.ua.tqs.fleet_simulator.services;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.fleet_simulator.config.SimulatorProperties;
import pt.ua.tqs.fleet_simulator.config.SimulatorProperties.VehicleSeed;
import pt.ua.tqs.fleet_simulator.domain.FaultEvent;
import pt.ua.tqs.fleet_simulator.domain.GeoPoint;
import pt.ua.tqs.fleet_simulator.domain.TelemetryReading;
import pt.ua.tqs.fleet_simulator.ports.RecordingTelemetryPublisher;

class FleetSimulatorTest {

    private static final Instant NOW = Instant.parse("2026-11-23T10:15:30Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Duration TICK = Duration.ofSeconds(5);
    private static final int DRAIN_PER_TICK = 2;
    private static final GeoPoint CENTRAL_SQUARE = new GeoPoint(40.6405, -8.6538);
    private static final GeoPoint RIVERSIDE = new GeoPoint(40.6420, -8.6500);

    private RecordingTelemetryPublisher publisher;
    private FleetSimulator simulator;

    @BeforeEach
    void setUp() {
        publisher = new RecordingTelemetryPublisher();
        simulator = new FleetSimulator(properties(DRAIN_PER_TICK,
                seed("EB-0001", 80), seed("EB-0002", 50)), publisher, CLOCK);
    }

    @Test
    @Requirement("FM-25")
    void publishesOneReadingPerVehicleEachRound() {
        simulator.publishRound();

        assertThat(publisher.drainReadings()).containsExactly(
                new TelemetryReading("EB-0001", NOW, 80, CENTRAL_SQUARE, true, null),
                new TelemetryReading("EB-0002", NOW, 50, CENTRAL_SQUARE, true, null));
    }

    @Test
    @Requirement("FM-25")
    void drainsOnlyTheVehiclesInUseEachRound() {
        simulator.unlock("EB-0001");
        publisher.drainReadings();

        simulator.publishRound();

        assertThat(publisher.drainReadings())
                .extracting(TelemetryReading::vehicleCode, TelemetryReading::batteryPercent)
                .containsExactly(
                        tuple("EB-0001", 80 - DRAIN_PER_TICK),
                        tuple("EB-0002", 50));
    }

    @Test
    @Requirement("FM-25")
    void unlockReportsTheOpenLockAtOnce() {
        simulator.unlock("EB-0001");

        assertThat(publisher.drainReadings()).containsExactly(
                new TelemetryReading("EB-0001", NOW, 80, CENTRAL_SQUARE, false, null));
    }

    @Test
    @Requirement("FM-25")
    void unlockingAnOpenLockReportsItsStateAgain() {
        simulator.unlock("EB-0001");

        simulator.unlock("EB-0001");

        assertThat(publisher.drainReadings())
                .hasSize(2)
                .allMatch(reading -> !reading.locked());
    }

    @Test
    @Requirement("FM-25")
    void lockReportsTheClosedLockAtOnce() {
        simulator.unlock("EB-0001");
        publisher.drainReadings();

        simulator.lock("EB-0001");

        assertThat(publisher.drainReadings()).containsExactly(
                new TelemetryReading("EB-0001", NOW, 80, CENTRAL_SQUARE, true, null));
    }

    @Test
    @Requirement("FM-25")
    void lockAtReportsTheVehicleDockedAtTheGivenPosition() {
        simulator.unlock("EB-0001");
        publisher.drainReadings();

        simulator.lockAt("EB-0001", RIVERSIDE);

        assertThat(publisher.drainReadings()).containsExactly(
                new TelemetryReading("EB-0001", NOW, 80, RIVERSIDE, true, null));
    }

    @Test
    @Requirement("FM-25")
    void triggeredFaultIsReportedAtOnceAndOnlyOnce() {
        simulator.triggerFault("EB-0001", FaultEvent.FLAT_TYRE);
        simulator.publishRound();

        assertThat(publisher.drainReadings())
                .extracting(TelemetryReading::vehicleCode, TelemetryReading::faultEvent)
                .containsExactly(
                        tuple("EB-0001", FaultEvent.FLAT_TYRE),
                        tuple("EB-0001", null),
                        tuple("EB-0002", null));
    }

    @Test
    @Requirement("FM-25")
    void ignoresMessagesForUnknownVehicles() {
        simulator.unlock("XX-9999");
        simulator.lock("XX-9999");
        simulator.lockAt("XX-9999", RIVERSIDE);
        simulator.triggerFault("XX-9999", FaultEvent.MOTOR_FAILURE);

        assertThat(publisher.drainReadings()).isEmpty();
    }

    @Test
    @Requirement("FM-25")
    void refusesFleetWithTheSameCodeTwice() {
        SimulatorProperties duplicated = properties(DRAIN_PER_TICK,
                seed("EB-0001", 80), seed("EB-0001", 50));

        assertThatThrownBy(() -> new FleetSimulator(duplicated, publisher, CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EB-0001");
    }

    @Test
    @Requirement("FM-25")
    void refusesNegativeBatteryDrain() {
        SimulatorProperties negativeDrain = properties(-1, seed("EB-0001", 80));

        assertThatThrownBy(() -> new FleetSimulator(negativeDrain, publisher, CLOCK))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static SimulatorProperties properties(int drainPerTick, VehicleSeed... fleet) {
        return new SimulatorProperties(TICK, drainPerTick, List.of(fleet));
    }

    private static VehicleSeed seed(String code, int batteryPercent) {
        return new VehicleSeed(code, batteryPercent,
                CENTRAL_SQUARE.latitude(), CENTRAL_SQUARE.longitude());
    }
}