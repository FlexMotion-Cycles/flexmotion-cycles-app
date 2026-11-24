package pt.ua.tqs.fleet_simulator.adapters.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

import pt.ua.tqs.fleet_simulator.config.MqttConfig;
import pt.ua.tqs.fleet_simulator.config.MqttProperties;
import pt.ua.tqs.fleet_simulator.domain.TelemetryReading;
import pt.ua.tqs.fleet_simulator.ports.TelemetryPublisher;
import tools.jackson.databind.json.JsonMapper;

/** Publishes each reading as JSON on the telemetry topic of its vehicle. */
@Component
public class MqttTelemetryPublisher implements TelemetryPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(MqttTelemetryPublisher.class);

    private final MessageChannel telemetryChannel;
    private final JsonMapper jsonMapper;
    private final String topicPrefix;

    public MqttTelemetryPublisher(
            @Qualifier(MqttConfig.TELEMETRY_CHANNEL) MessageChannel telemetryChannel,
            JsonMapper jsonMapper, MqttProperties properties) {
        this.telemetryChannel = telemetryChannel;
        this.jsonMapper = jsonMapper;
        this.topicPrefix = properties.telemetryTopicPrefix() + MqttConfig.TOPIC_LEVEL_SEPARATOR;
    }

    /** A reading that cannot be delivered is logged and lost: the next tick sends a new one. */
    @Override
    public void publish(TelemetryReading reading) {
        Message<String> message = MessageBuilder
                .withPayload(jsonMapper.writeValueAsString(TelemetryMessage.from(reading)))
                .setHeader(MqttHeaders.TOPIC, topicPrefix + reading.vehicleCode())
                .build();
        try {
            telemetryChannel.send(message);
        } catch (MessagingException e) {
            LOG.warn("Could not publish the reading of {}: {}", reading.vehicleCode(),
                    e.getMessage());
        }
    }
}