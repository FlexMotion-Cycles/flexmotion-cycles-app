package pt.ua.tqs.fleet_simulator.ports;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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

    /** Waits until {@code count} readings arrive (or the timeout ends) and returns them. */
    public List<TelemetryReading> awaitReadings(int count, Duration timeout)
            throws InterruptedException {
        List<TelemetryReading> published = new ArrayList<>();
        long deadline = System.nanoTime() + timeout.toNanos();
        while (published.size() < count) {
            long remaining = deadline - System.nanoTime();
            TelemetryReading reading = readings.poll(remaining, TimeUnit.NANOSECONDS);
            if (reading == null) {
                break;
            }
            published.add(reading);
        }
        return published;
    }
}