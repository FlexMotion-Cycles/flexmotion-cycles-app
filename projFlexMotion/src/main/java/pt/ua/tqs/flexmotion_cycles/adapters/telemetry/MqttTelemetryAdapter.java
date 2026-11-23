package pt.ua.tqs.flexmotion_cycles.adapters.telemetry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;

import pt.ua.tqs.flexmotion_cycles.domain.TelemetryReading;
import pt.ua.tqs.flexmotion_cycles.ports.TelemetryPort;

public class MqttTelemetryAdapter implements TelemetryPort {

    private static final Logger log = LoggerFactory.getLogger(MqttTelemetryAdapter.class);
    private static final String UNLOCK_COMMAND_PAYLOAD = "{\"command\":\"UNLOCK\"}";

    private final TelemetryMessageParser parser;
    private final ApplicationEventPublisher eventPublisher;
    private final MessageChannel commandChannel;
    private final String commandTopicPrefix;

    public MqttTelemetryAdapter(TelemetryMessageParser parser,
            ApplicationEventPublisher eventPublisher, MessageChannel commandChannel,
            String commandTopicPrefix) {
        this.parser = parser;
        this.eventPublisher = eventPublisher;
        this.commandChannel = commandChannel;
        this.commandTopicPrefix = commandTopicPrefix;
    }

    @Override
    public void sendUnlockCommand(String vehicleCode) {
        commandChannel.send(MessageBuilder.withPayload(UNLOCK_COMMAND_PAYLOAD)
                .setHeader(MqttHeaders.TOPIC, commandTopicPrefix + "/" + vehicleCode)
                .build());
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
