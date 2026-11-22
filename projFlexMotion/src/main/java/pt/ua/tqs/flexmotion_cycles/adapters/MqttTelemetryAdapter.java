package pt.ua.tqs.flexmotion_cycles.adapters;

import java.util.function.Consumer;

import pt.ua.tqs.flexmotion_cycles.domain.TelemetryReading;
import pt.ua.tqs.flexmotion_cycles.ports.TelemetryPort;

public class MqttTelemetryAdapter implements TelemetryPort {

    private final TelemetryMessageParser parser;

    public MqttTelemetryAdapter(TelemetryMessageParser parser) {
        this.parser = parser;
    }

    @Override
    public void subscribe(Consumer<TelemetryReading> consumer) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    void handle(String payload) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
