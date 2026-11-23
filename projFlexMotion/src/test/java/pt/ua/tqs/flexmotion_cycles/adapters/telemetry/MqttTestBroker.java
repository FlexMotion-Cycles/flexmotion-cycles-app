package pt.ua.tqs.flexmotion_cycles.adapters.telemetry;

import java.nio.charset.StandardCharsets;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

final class MqttTestBroker {

    static final String TOPIC_PREFIX = "flexmotion/telemetry";
    static final String COMMAND_TOPIC_PREFIX = "flexmotion/commands";
    static final int QOS_AT_LEAST_ONCE = 1;

    private static final String RABBITMQ_IMAGE = "rabbitmq:4.1-management-alpine";
    private static final int MQTT_PORT = 1883;
    private static final String USERNAME = "telemetry-it";
    private static final String PASSWORD = "telemetry-it";

    private static final GenericContainer<?> RABBITMQ =
            new GenericContainer<>(DockerImageName.parse(RABBITMQ_IMAGE))
                    .withEnv("RABBITMQ_DEFAULT_USER", USERNAME)
                    .withEnv("RABBITMQ_DEFAULT_PASS", PASSWORD)
                    .withEnv("RABBITMQ_ENABLED_PLUGINS", "rabbitmq_mqtt")
                    .withEnv("RABBITMQ_ENABLED_PLUGINS_FILE", "/var/lib/rabbitmq/enabled_plugins")
                    .withExposedPorts(MQTT_PORT)
                    .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

    static {
        RABBITMQ.start();
    }

    private MqttTestBroker() {
    }

    static void registerProperties(DynamicPropertyRegistry registry, String clientId) {
        registry.add("flexmotion.telemetry.adapter", () -> "mqtt");
        registry.add("flexmotion.telemetry.broker-url", MqttTestBroker::brokerUrl);
        registry.add("flexmotion.telemetry.client-id", () -> clientId);
        registry.add("flexmotion.telemetry.username", () -> USERNAME);
        registry.add("flexmotion.telemetry.password", () -> PASSWORD);
        registry.add("flexmotion.telemetry.topic-prefix", () -> TOPIC_PREFIX);
        registry.add("flexmotion.telemetry.command-topic-prefix", () -> COMMAND_TOPIC_PREFIX);
    }

    static void publish(String vehicleCode, String payload) throws MqttException {
        try (MqttClient client = connect("telemetry-it-publisher")) {
            client.publish(TOPIC_PREFIX + "/" + vehicleCode,
                    payload.getBytes(StandardCharsets.UTF_8), QOS_AT_LEAST_ONCE, false);
            client.disconnect();
        }
    }

    static MqttClient connect(String clientId) throws MqttException {
        MqttClient client = new MqttClient(brokerUrl(), clientId, new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(USERNAME);
        options.setPassword(PASSWORD.toCharArray());
        client.connect(options);
        return client;
    }

    private static String brokerUrl() {
        return "tcp://" + RABBITMQ.getHost() + ":" + RABBITMQ.getMappedPort(MQTT_PORT);
    }
}
