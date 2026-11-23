package pt.ua.tqs.fleet_simulator.ports;

import pt.ua.tqs.fleet_simulator.domain.TelemetryReading;

/** Outbound port: where the readings go (the MQTT broker, or a fake in tests). */
public interface TelemetryPublisher {

    void publish(TelemetryReading reading);
}