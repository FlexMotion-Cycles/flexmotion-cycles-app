package pt.ua.tqs.fleet_simulator;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs the simulator against RabbitMQ with the MQTT plugin, as in Docker Compose, and talks to
 * it only over MQTT, as the platform does. The context is shared by the tests, so each test
 * uses a different vehicle.
 */
@Testcontainers
@SpringBootTest(properties = "simulator.tick-interval=200ms")
@DirtiesContext
class FleetSimulatorApplicationIT {

    private static final String BROKER_USER = "simulator";
    private static final String BROKER_PASSWORD = "integration-test-only";
    private static final int MQTT_PORT = 1883;
    private static final int QOS_AT_LEAST_ONCE = 1;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final List<String> CONTRACT_FIELDS = List.of(
            "vehicleCode", "timestamp", "batteryPercent", "position", "locked", "faultEvent");
    private static final String UNLOCK = "{\"command\": \"UNLOCK\"}";

    @Container
    static final GenericContainer<?> RABBITMQ =
            new GenericContainer<>(DockerImageName.parse("rabbitmq:4.1-management-alpine"))
                    .withEnv("RABBITMQ_DEFAULT_USER", BROKER_USER)
                    .withEnv("RABBITMQ_DEFAULT_PASS", BROKER_PASSWORD)
                    // As in Docker Compose: the default plugins file is read-only.
                    .withEnv("RABBITMQ_ENABLED_PLUGINS", "rabbitmq_mqtt")
                    .withEnv("RABBITMQ_ENABLED_PLUGINS_FILE", "/var/lib/rabbitmq/enabled_plugins")
                    .withExposedPorts(MQTT_PORT)
                    .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

    @DynamicPropertySource
    static void brokerProperties(DynamicPropertyRegistry registry) {
        registry.add("simulator.mqtt.broker-url", FleetSimulatorApplicationIT::brokerUrl);
        registry.add("simulator.mqtt.username", () -> BROKER_USER);
        registry.add("simulator.mqtt.password", () -> BROKER_PASSWORD);
    }

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final BlockingQueue<Received> received = new LinkedBlockingQueue<>();
    private MqttClient platform;

    @BeforeEach
    void connectAsThePlatform() throws MqttException {
        platform = new MqttClient(brokerUrl(), "platform-it", new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(BROKER_USER);
        options.setPassword(BROKER_PASSWORD.toCharArray());
        platform.connect(options);
        platform.subscribe("flexmotion/telemetry/+", QOS_AT_LEAST_ONCE, (topic, message) ->
                received.add(new Received(topic,
                        new String(message.getPayload(), StandardCharsets.UTF_8))));
    }

    @AfterEach
    void disconnect() throws MqttException {
        platform.disconnect();
        platform.close();
    }

    @Test
    @Requirement("FM-25")
    void everyVehicleOfTheConfiguredFleetReportsOnSchedule() throws InterruptedException {
        for (String vehicleCode : List.of("EB-0001", "EB-0002", "CB-0001")) {
            JsonNode reading = awaitReading(vehicleCode, any -> true)
                    .orElseThrow(() -> new AssertionError("No reading of " + vehicleCode));

            assertThat(reading.properties()).extracting(Map.Entry::getKey)
                    .containsExactlyInAnyOrderElementsOf(CONTRACT_FIELDS);
            assertThat(reading.get("vehicleCode").asString()).isEqualTo(vehicleCode);
            assertThat(Instant.parse(reading.get("timestamp").asString())).isNotNull();
        }
    }

    @Test
    @Requirement("FM-25")
    void unlockCommandOpensTheLockAndTheVehicleReportsIt() throws Exception {
        publish("flexmotion/commands/EB-0001", UNLOCK);

        assertThat(awaitReading("EB-0001", reading -> !reading.get("locked").asBoolean()))
                .as("EB-0001 reports an open lock").isPresent();
    }

    @Test
    @Requirement("FM-25")
    void malformedCommandIsDiscardedAndTheSimulatorKeepsListening() throws Exception {
        publish("flexmotion/commands/EB-0002", "not json");
        publish("flexmotion/commands/EB-0002", UNLOCK);

        assertThat(awaitReading("EB-0002", reading -> !reading.get("locked").asBoolean()))
                .as("EB-0002 reports an open lock").isPresent();
    }

    @Test
    @Requirement("FM-25")
    void triggerFaultMakesTheVehicleReportTheFault() throws Exception {
        publish("flexmotion/simulator/CB-0001",
                "{\"action\": \"TRIGGER_FAULT\", \"faultEvent\": \"BRAKE_FAILURE\"}");

        assertThat(awaitReading("CB-0001",
                reading -> "BRAKE_FAILURE".equals(reading.get("faultEvent").asString())))
                .as("CB-0001 reports BRAKE_FAILURE").isPresent();
    }

    private static String brokerUrl() {
        return "tcp://" + RABBITMQ.getHost() + ":" + RABBITMQ.getMappedPort(MQTT_PORT);
    }

    private void publish(String topic, String payload) throws MqttException {
        platform.publish(topic, payload.getBytes(StandardCharsets.UTF_8), QOS_AT_LEAST_ONCE,
                false);
    }

    /** Waits for a reading of the vehicle that matches the condition; others are skipped. */
    private Optional<JsonNode> awaitReading(String vehicleCode, Predicate<JsonNode> condition)
            throws InterruptedException {
        String topic = "flexmotion/telemetry/" + vehicleCode;
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        for (long left = TIMEOUT.toNanos(); left > 0; left = deadline - System.nanoTime()) {
            Received message = received.poll(left, TimeUnit.NANOSECONDS);
            if (message != null && message.topic().equals(topic)) {
                JsonNode reading = jsonMapper.readTree(message.payload());
                if (condition.test(reading)) {
                    return Optional.of(reading);
                }
            }
        }
        return Optional.empty();
    }

    private record Received(String topic, String payload) {
    }
}