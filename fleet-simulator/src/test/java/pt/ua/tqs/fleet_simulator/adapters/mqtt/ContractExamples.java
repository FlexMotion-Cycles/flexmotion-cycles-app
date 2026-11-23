package pt.ua.tqs.fleet_simulator.adapters.mqtt;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import pt.ua.tqs.fleet_simulator.config.MqttProperties;

/** The examples of the telemetry contract (src/test/resources/contract) and its topics. */
final class ContractExamples {

    static final MqttProperties MQTT_PROPERTIES = new MqttProperties(
            "tcp://localhost:1883", "", "", "fleet-simulator",
            "flexmotion/telemetry", "flexmotion/commands", "flexmotion/simulator");

    private ContractExamples() {
    }

    static String read(String fileName) {
        try (InputStream in = ContractExamples.class.getResourceAsStream("/contract/" + fileName)) {
            return new String(Objects.requireNonNull(in, fileName).readAllBytes(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}