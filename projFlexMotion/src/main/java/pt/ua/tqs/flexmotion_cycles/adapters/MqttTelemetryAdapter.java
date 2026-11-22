package pt.ua.tqs.flexmotion_cycles.adapters;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ua.tqs.flexmotion_cycles.domain.TelemetryReading;
import pt.ua.tqs.flexmotion_cycles.ports.TelemetryPort;

public class MqttTelemetryAdapter implements TelemetryPort {

    private static final Logger log = LoggerFactory.getLogger(MqttTelemetryAdapter.class);

    private final TelemetryMessageParser parser;
    private final List<Consumer<TelemetryReading>> consumers = new CopyOnWriteArrayList<>();

    public MqttTelemetryAdapter(TelemetryMessageParser parser) {
        this.parser = parser;
    }

    @Override
    public void subscribe(Consumer<TelemetryReading> consumer) {
        consumers.add(consumer);
    }

    void handle(String payload) {
        TelemetryReading reading;
        try {
            reading = parser.parse(payload);
        } catch (InvalidTelemetryMessageException e) {
            log.warn("Discarded telemetry message: {}", e.getMessage());
            return;
        }
        consumers.forEach(consumer -> consumer.accept(reading));
    }
}
