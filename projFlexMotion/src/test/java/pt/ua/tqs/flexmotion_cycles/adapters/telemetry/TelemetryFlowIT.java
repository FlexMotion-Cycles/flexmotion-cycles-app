package pt.ua.tqs.flexmotion_cycles.adapters.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;
import pt.ua.tqs.flexmotion_cycles.data.MaintenanceTicketRepository;
import pt.ua.tqs.flexmotion_cycles.data.StationRepository;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.StationType;
import pt.ua.tqs.flexmotion_cycles.domain.TicketOrigin;
import pt.ua.tqs.flexmotion_cycles.domain.TicketStatus;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleState;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TelemetryFlowIT {

    private static final String VEHICLE_CODE = "EB-FLOW";
    private static final double LATITUDE = 40.6405;
    private static final double LONGITUDE = -8.6538;
    private static final int INITIAL_BATTERY = 80;
    private static final Duration PROCESSING_TIMEOUT = Duration.ofSeconds(10);
    private static final String FAULT_READING = """
            {
              "vehicleCode": "EB-FLOW",
              "timestamp": "2026-11-22T16:05:00Z",
              "batteryPercent": 54,
              "position": { "latitude": 40.6405, "longitude": -8.6538 },
              "locked": true,
              "faultEvent": "BRAKE_FAILURE"
            }
            """;

    @DynamicPropertySource
    static void telemetryProperties(DynamicPropertyRegistry registry) {
        MqttTestBroker.registerProperties(registry, "flexmotion-backend-flow-it");
    }

    private final StationRepository stationRepository;
    private final VehicleRepository vehicleRepository;
    private final MaintenanceTicketRepository maintenanceTicketRepository;
    private final TransactionTemplate transactionTemplate;

    private Station station;
    private Vehicle vehicle;

    @Autowired
    TelemetryFlowIT(StationRepository stationRepository, VehicleRepository vehicleRepository,
            MaintenanceTicketRepository maintenanceTicketRepository,
            TransactionTemplate transactionTemplate) {
        this.stationRepository = stationRepository;
        this.vehicleRepository = vehicleRepository;
        this.maintenanceTicketRepository = maintenanceTicketRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @BeforeEach
    void setUp() {
        station = stationRepository.save(new Station("Telemetry Flow", LATITUDE, LONGITUDE, 10,
                StationType.CONVENTIONAL));
        vehicle = vehicleRepository.save(
                new Vehicle(VEHICLE_CODE, VehicleType.E_BIKE, INITIAL_BATTERY, station));
    }

    @AfterEach
    void cleanUp() {
        maintenanceTicketRepository.deleteAll();
        vehicleRepository.deleteById(vehicle.getId());
        stationRepository.deleteById(station.getId());
    }

    @Test
    @Requirement("FM-26")
    void faultEventOverMqttUpdatesVehicleAndOpensTicket() throws Exception {
        MqttTestBroker.publish(VEHICLE_CODE, FAULT_READING);

        await().atMost(PROCESSING_TIMEOUT).untilAsserted(() ->
                transactionTemplate.executeWithoutResult(status -> {
                    Vehicle updated = vehicleRepository.findByCode(VEHICLE_CODE).orElseThrow();
                    assertThat(updated.getState()).isEqualTo(VehicleState.MAINTENANCE);
                    assertThat(updated.getBatteryLevel()).isEqualTo(54);
                    assertThat(updated.isLocked()).isTrue();
                    assertThat(updated.getStation().getId()).isEqualTo(station.getId());
                }));
        assertThat(maintenanceTicketRepository.existsByVehicleAndOriginAndStatus(
                vehicle, TicketOrigin.FAULT_EVENT, TicketStatus.OPEN)).isTrue();
    }
}
