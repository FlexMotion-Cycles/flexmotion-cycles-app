package pt.ua.tqs.fleet_simulator.ports;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import pt.ua.tqs.fleet_simulator.domain.TelemetryReading;

/** Fake of the telemetry port: keeps every reading in memory for the tests to inspect. */
public class RecordingTelemetryPublisher implements TelemetryPublisher {

    private final BlockingQueue<TelemetryReading> readings = new LinkedBlockingQueue<>();

    @Override
    public void publish(TelemetryReading reading) {
        readings.add(reading);
    }

    /** Removes and returns everything published so far. */
    public List<TelemetryReading> drainReadings() {
        List<TelemetryReading> published = new ArrayList<>();
        readings.drainTo(published);
        return published;
    }
}