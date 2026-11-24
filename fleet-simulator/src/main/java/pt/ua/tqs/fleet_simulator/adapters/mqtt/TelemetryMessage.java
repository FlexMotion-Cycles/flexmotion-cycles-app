package pt.ua.tqs.fleet_simulator.adapters.mqtt;

import java.time.Instant;

import pt.ua.tqs.fleet_simulator.domain.FaultEvent;
import pt.ua.tqs.fleet_simulator.domain.TelemetryReading;

/** A telemetry reading as the contract writes it. Every field is always present. */
record TelemetryMessage(
        String vehicleCode,
        Instant timestamp,
        int batteryPercent,
        PositionMessage position,
        boolean locked,
        FaultEvent faultEvent) {

    static TelemetryMessage from(TelemetryReading reading) {
        return new TelemetryMessage(reading.vehicleCode(), reading.timestamp(),
                reading.batteryPercent(), PositionMessage.from(reading.position()),
                reading.locked(), reading.faultEvent());
    }
}