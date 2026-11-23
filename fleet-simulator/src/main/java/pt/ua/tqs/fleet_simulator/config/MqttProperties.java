package pt.ua.tqs.fleet_simulator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** MQTT broker connection and the topic prefixes of the telemetry contract. */
@ConfigurationProperties(prefix = "simulator.mqtt")
public record MqttProperties(
        String brokerUrl,
        String username,
        String password,
        String clientId,
        String telemetryTopicPrefix,
        String commandTopicPrefix,
        String controlTopicPrefix) {
}