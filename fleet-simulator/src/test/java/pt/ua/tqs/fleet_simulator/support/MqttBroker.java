package pt.ua.tqs.fleet_simulator.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * RabbitMQ with the MQTT plugin, set up as in Docker Compose. It starts on first use and is
 * shared by the integration and acceptance tests; Testcontainers removes it when they end.
 */
public final class MqttBroker {

    /** Synthetic credentials, valid only in this container. */
    public static final String USER = "simulator";
    public static final String PASSWORD = "integration-test-only";

    private static final int MQTT_PORT = 1883;
    private static final GenericContainer<?> RABBITMQ =
            new GenericContainer<>(DockerImageName.parse("rabbitmq:4.1-management-alpine"))
                    .withEnv("RABBITMQ_DEFAULT_USER", USER)
                    .withEnv("RABBITMQ_DEFAULT_PASS", PASSWORD)
                    // As in Docker Compose: the default plugins file is read-only.
                    .withEnv("RABBITMQ_ENABLED_PLUGINS", "rabbitmq_mqtt")
                    .withEnv("RABBITMQ_ENABLED_PLUGINS_FILE", "/var/lib/rabbitmq/enabled_plugins")
                    .withExposedPorts(MQTT_PORT)
                    .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

    private MqttBroker() {
    }

    /** The MQTT address of the broker, which starts on the first call. */
    public static synchronized String url() {
        if (!RABBITMQ.isRunning()) {
            RABBITMQ.start();
        }
        return "tcp://" + RABBITMQ.getHost() + ":" + RABBITMQ.getMappedPort(MQTT_PORT);
    }

    /** Points the simulator under test at this broker. */
    public static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("simulator.mqtt.broker-url", MqttBroker::url);
        registry.add("simulator.mqtt.username", () -> USER);
        registry.add("simulator.mqtt.password", () -> PASSWORD);
    }
}