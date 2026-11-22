package pt.ua.tqs.flexmotion_cycles.ports;

import java.util.function.Consumer;

import pt.ua.tqs.flexmotion_cycles.domain.TelemetryReading;

public interface TelemetryPort {

    void subscribe(Consumer<TelemetryReading> consumer);
}
