package pt.ua.tqs.flexmotion_cycles.adapters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import pt.ua.tqs.flexmotion_cycles.domain.TelemetryReading;

public class MqttTelemetryAdapter {

    private static final Logger log = LoggerFactory.getLogger(MqttTelemetryAdapter.class);

    private final TelemetryMessageParser parser;
    private final ApplicationEventPublisher eventPublisher;

    public MqttTelemetryAdapter(TelemetryMessageParser parser,
            ApplicationEventPublisher eventPublisher) {
        this.parser = parser;
        this.eventPublisher = eventPublisher;
    }

    void handle(String payload) {
        TelemetryReading reading;
        try {
            reading = parser.parse(payload);
        } catch (InvalidTelemetryMessageException e) {
            log.warn("Discarded telemetry message: {}", e.getMessage());
            return;
        }
        eventPublisher.publishEvent(reading);
    }
}
