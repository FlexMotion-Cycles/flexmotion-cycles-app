package pt.ua.tqs.fleet_simulator;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.fleet_simulator.support.MqttBroker;
import pt.ua.tqs.fleet_simulator.support.PlatformMqttClient;
import tools.jackson.databind.JsonNode;

/**
 * Runs the simulator against RabbitMQ with the MQTT plugin, as in Docker Compose, and talks to
 * it only over MQTT, as the platform does. The context is shared by the tests, so each test
 * uses a different vehicle. It is closed after the class, so this simulator stops publishing
 * before other tests use the same broker.
 */
@SpringBootTest(properties = "simulator.tick-interval=200ms")
@DirtiesContext
class FleetSimulatorApplicationIT {

    private static final List<String> CONTRACT_FIELDS = List.of(
            "vehicleCode", "timestamp", "batteryPercent", "position", "locked", "faultEvent");
    private static final String UNLOCK = "{\"command\": \"UNLOCK\"}";

    @DynamicPropertySource
    static void brokerProperties(DynamicPropertyRegistry registry) {
        MqttBroker.registerProperties(registry);
    }

    private PlatformMqttClient platform;

    @BeforeEach
    void connectAsThePlatform() throws MqttException {
        platform = new PlatformMqttClient("platform-it");
    }

    @AfterEach
    void disconnect() throws MqttException {
        platform.close();
    }

    @Test
    @Requirement("FM-25")
    void everyVehicleOfTheConfiguredFleetReportsOnSchedule() throws InterruptedException {
        for (String vehicleCode : List.of("EB-0001", "EB-0002", "CB-0001")) {
            JsonNode reading = platform.awaitReading(vehicleCode, any -> true)
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
        platform.publish("flexmotion/commands/EB-0001", UNLOCK);

        assertThat(platform.awaitReading("EB-0001", reading -> !reading.get("locked").asBoolean()))
                .as("EB-0001 reports an open lock").isPresent();
    }

    @Test
    @Requirement("FM-25")
    void malformedCommandIsDiscardedAndTheSimulatorKeepsListening() throws Exception {
        platform.publish("flexmotion/commands/EB-0002", "not json");
        platform.publish("flexmotion/commands/EB-0002", UNLOCK);

        assertThat(platform.awaitReading("EB-0002", reading -> !reading.get("locked").asBoolean()))
                .as("EB-0002 reports an open lock").isPresent();
    }

    @Test
    @Requirement("FM-25")
    void triggerFaultMakesTheVehicleReportTheFault() throws Exception {
        platform.publish("flexmotion/simulator/CB-0001",
                "{\"action\": \"TRIGGER_FAULT\", \"faultEvent\": \"BRAKE_FAILURE\"}");

        assertThat(platform.awaitReading("CB-0001",
                reading -> "BRAKE_FAILURE".equals(reading.get("faultEvent").asString())))
                .as("CB-0001 reports BRAKE_FAILURE").isPresent();
    }
}