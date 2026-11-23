package pt.ua.tqs.flexmotion_cycles.adapters;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flexmotion.telemetry")
record TelemetryProperties(
        String brokerUrl,
        String clientId,
        String username,
        String password,
        String topicPrefix,
        String commandTopicPrefix) {
}
