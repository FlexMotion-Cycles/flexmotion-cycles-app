package pt.ua.tqs.flexmotion_cycles.adapters;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;
import pt.ua.tqs.flexmotion_cycles.domain.GeoPoint;
import pt.ua.tqs.flexmotion_cycles.domain.TelemetryReading;

@SpringBootTest
@Import({TestcontainersConfiguration.class, MqttTelemetryAdapterIT.CollectorConfig.class})
class MqttTelemetryAdapterIT {

    private static final String RABBITMQ_IMAGE = "rabbitmq:4.1-management-alpine";
    private static final int MQTT_PORT = 1883;
    private static final String USERNAME = "telemetry-it";
    private static final String PASSWORD = "telemetry-it";
    private static final String TOPIC_PREFIX = "flexmotion/telemetry";
    private static final String VEHICLE_CODE = "EB-0001";
    private static final int QOS_AT_LEAST_ONCE = 1;
    private static final long RECEIVE_TIMEOUT_SECONDS = 10;

    static final GenericContainer<?> RABBITMQ =
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

    @DynamicPropertySource
    static void telemetryProperties(DynamicPropertyRegistry registry) {
        registry.add("flexmotion.telemetry.enabled", () -> "true");
        registry.add("flexmotion.telemetry.broker-url", MqttTelemetryAdapterIT::brokerUrl);
        registry.add("flexmotion.telemetry.client-id", () -> "flexmotion-backend-it");
        registry.add("flexmotion.telemetry.username", () -> USERNAME);
        registry.add("flexmotion.telemetry.password", () -> PASSWORD);
        registry.add("flexmotion.telemetry.topic-prefix", () -> TOPIC_PREFIX);
    }

    private final TelemetryReadingCollector collector;

    @Autowired
    MqttTelemetryAdapterIT(TelemetryReadingCollector collector) {
        this.collector = collector;
    }

    @BeforeEach
    void clearReadings() {
        collector.readings.clear();
    }

    @Test
    @Requirement("FM-26")
    void receivesReadingInTheAgreedFormat() throws Exception {
        publish(TelemetryFixtures.read("valid-reading.json"));

        assertThat(nextReading()).isEqualTo(new TelemetryReading(VEHICLE_CODE,
                Instant.parse("2026-11-22T16:05:00Z"), 19,
                new GeoPoint(40.6405, -8.6538), true, null));
    }

    @Test
    @Requirement("FM-26")
    void discardsInvalidMessageAndKeepsConsuming() throws Exception {
        publish("{not json");
        publish(TelemetryFixtures.read("valid-reading.json"));

        assertThat(nextReading().vehicleCode()).isEqualTo(VEHICLE_CODE);
        assertThat(collector.readings).isEmpty();
    }

    private TelemetryReading nextReading() throws InterruptedException {
        TelemetryReading reading =
                collector.readings.poll(RECEIVE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(reading).as("reading delivered by the telemetry port").isNotNull();
        return reading;
    }

    private static void publish(String payload) throws Exception {
        try (MqttClient client = new MqttClient(brokerUrl(), "telemetry-it-publisher",
                new MemoryPersistence())) {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(USERNAME);
            options.setPassword(PASSWORD.toCharArray());
            client.connect(options);
            client.publish(TOPIC_PREFIX + "/" + VEHICLE_CODE,
                    payload.getBytes(StandardCharsets.UTF_8), QOS_AT_LEAST_ONCE, false);
            client.disconnect();
        }
    }

    private static String brokerUrl() {
        return "tcp://" + RABBITMQ.getHost() + ":" + RABBITMQ.getMappedPort(MQTT_PORT);
    }

    @TestConfiguration
    static class CollectorConfig {

        @Bean
        TelemetryReadingCollector telemetryReadingCollector() {
            return new TelemetryReadingCollector();
        }
    }

    static class TelemetryReadingCollector {

        private final BlockingQueue<TelemetryReading> readings = new LinkedBlockingQueue<>();

        @EventListener
        void onReading(TelemetryReading reading) {
            readings.add(reading);
        }
    }
}
