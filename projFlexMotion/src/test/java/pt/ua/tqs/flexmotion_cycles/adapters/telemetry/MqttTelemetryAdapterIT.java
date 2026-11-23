package pt.ua.tqs.flexmotion_cycles.adapters.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.MqttClient;
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

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;
import pt.ua.tqs.flexmotion_cycles.domain.GeoPoint;
import pt.ua.tqs.flexmotion_cycles.domain.TelemetryReading;
import pt.ua.tqs.flexmotion_cycles.ports.TelemetryPort;

@SpringBootTest
@Import({TestcontainersConfiguration.class, MqttTelemetryAdapterIT.CollectorConfig.class})
class MqttTelemetryAdapterIT {

    private static final String VEHICLE_CODE = "EB-0001";
    private static final long RECEIVE_TIMEOUT_SECONDS = 10;

    @DynamicPropertySource
    static void telemetryProperties(DynamicPropertyRegistry registry) {
        MqttTestBroker.registerProperties(registry, "flexmotion-backend-it");
    }

    private final TelemetryReadingCollector collector;
    private final TelemetryPort telemetryPort;

    @Autowired
    MqttTelemetryAdapterIT(TelemetryReadingCollector collector, TelemetryPort telemetryPort) {
        this.collector = collector;
        this.telemetryPort = telemetryPort;
    }

    @BeforeEach
    void clearReadings() {
        collector.readings.clear();
    }

    @Test
    @Requirement("FM-26")
    void receivesReadingInTheAgreedFormat() throws Exception {
        MqttTestBroker.publish(VEHICLE_CODE, TelemetryFixtures.read("valid-reading.json"));

        assertThat(nextReading()).isEqualTo(new TelemetryReading(VEHICLE_CODE,
                Instant.parse("2026-11-22T16:05:00Z"), 19,
                new GeoPoint(40.6405, -8.6538), true, null));
    }

    @Test
    @Requirement("FM-26")
    void discardsInvalidMessageAndKeepsConsuming() throws Exception {
        MqttTestBroker.publish(VEHICLE_CODE, "{not json");
        MqttTestBroker.publish(VEHICLE_CODE, TelemetryFixtures.read("valid-reading.json"));

        assertThat(nextReading().vehicleCode()).isEqualTo(VEHICLE_CODE);
        assertThat(collector.readings).isEmpty();
    }

    @Test
    @Requirement("FM-26")
    void sendsUnlockCommandThroughTheBroker() throws Exception {
        BlockingQueue<String> commands = new LinkedBlockingQueue<>();
        try (MqttClient vehicle = MqttTestBroker.connect("vehicle-it-subscriber")) {
            vehicle.subscribe(MqttTestBroker.COMMAND_TOPIC_PREFIX + "/" + VEHICLE_CODE,
                    MqttTestBroker.QOS_AT_LEAST_ONCE,
                    (topic, message) -> commands.add(
                            new String(message.getPayload(), StandardCharsets.UTF_8)));

            telemetryPort.sendUnlockCommand(VEHICLE_CODE);

            assertThat(commands.poll(RECEIVE_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                    .isEqualTo("{\"command\":\"UNLOCK\"}");
            vehicle.disconnect();
        }
    }

    private TelemetryReading nextReading() throws InterruptedException {
        TelemetryReading reading =
                collector.readings.poll(RECEIVE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(reading).as("reading delivered by the telemetry port").isNotNull();
        return reading;
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
