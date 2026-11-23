package pt.ua.tqs.fleet_simulator;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.fleet_simulator.domain.TelemetryReading;
import pt.ua.tqs.fleet_simulator.ports.RecordingTelemetryPublisher;

/** Starts the simulator with its real application.yml and a fake publisher. */
@SpringBootTest(properties = "simulator.tick-interval=100ms")
class FleetSimulatorApplicationIT {

    private static final int FLEET_SIZE = 3;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @TestConfiguration
    static class FakePublisherConfig {

        @Bean
        RecordingTelemetryPublisher telemetryPublisher() {
            return new RecordingTelemetryPublisher();
        }
    }

    @Autowired
    private RecordingTelemetryPublisher publisher;

    @Test
    @Requirement("FM-25")
    void everyVehicleOfTheConfiguredFleetReportsOnSchedule() throws InterruptedException {
        assertThat(publisher.awaitReadings(FLEET_SIZE, TIMEOUT))
                .extracting(TelemetryReading::vehicleCode)
                .containsExactly("EB-0001", "EB-0002", "CB-0001");
    }
}