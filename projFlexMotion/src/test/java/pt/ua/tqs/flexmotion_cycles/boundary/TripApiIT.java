package pt.ua.tqs.flexmotion_cycles.boundary;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;
import pt.ua.tqs.flexmotion_cycles.adapters.telemetry.FakeTelemetryAdapter;
import pt.ua.tqs.flexmotion_cycles.data.MaintenanceTicketRepository;
import pt.ua.tqs.flexmotion_cycles.data.StationRepository;
import pt.ua.tqs.flexmotion_cycles.data.TripRepository;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.StationType;
import pt.ua.tqs.flexmotion_cycles.domain.TripStatus;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("it")
class TripApiIT {

    private final StationRepository stationRepository;
    private final VehicleRepository vehicleRepository;
    private final MaintenanceTicketRepository maintenanceTicketRepository;
    private final TripRepository tripRepository;
    private final FakeTelemetryAdapter telemetry;
    private final int port;
    private final String username;
    private final String password;

    private Station central;

    @Autowired
    TripApiIT(StationRepository stationRepository, VehicleRepository vehicleRepository,
            MaintenanceTicketRepository maintenanceTicketRepository,
            TripRepository tripRepository, FakeTelemetryAdapter telemetry,
            @Value("${local.server.port}") int port,
            @Value("${spring.security.user.name}") String username,
            @Value("${spring.security.user.password}") String password) {
        this.stationRepository = stationRepository;
        this.vehicleRepository = vehicleRepository;
        this.maintenanceTicketRepository = maintenanceTicketRepository;
        this.tripRepository = tripRepository;
        this.telemetry = telemetry;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    @BeforeEach
    void setUp() {
        tripRepository.deleteAll();
        maintenanceTicketRepository.deleteAll();
        vehicleRepository.deleteAll();
        stationRepository.deleteAll();
        telemetry.reset();
        central = stationRepository.save(
                new Station("Central", 40.6405, -8.6538, 10, StationType.CONVENTIONAL));
    }

    @Test
    @Requirement("FM-27")
    void unlockIsAcceptedAndTheTripIsStored() {
        vehicleRepository.save(new Vehicle("EB-01", VehicleType.E_BIKE, 80, central));

        unlock("EB-01")
                .statusCode(202)
                .body("vehicleCode", equalTo("EB-01"))
                .body("status", equalTo("UNLOCK_REQUESTED"))
                .body("startedAt", nullValue());

        assertThat(tripRepository.findByVehicleCodeAndStatus("EB-01", TripStatus.UNLOCK_REQUESTED))
                .hasValueSatisfying(trip -> assertThat(trip.getUsername()).isEqualTo(username));
        assertThat(telemetry.unlockCommands()).containsExactly("EB-01");
    }

    @Test
    @Requirement("FM-27")
    void userWithATripInProgressCannotUnlockAnother() {
        vehicleRepository.save(new Vehicle("EB-01", VehicleType.E_BIKE, 80, central));
        vehicleRepository.save(new Vehicle("EB-02", VehicleType.E_BIKE, 80, central));

        unlock("EB-01").statusCode(202);
        unlock("EB-02").statusCode(409);

        assertThat(telemetry.unlockCommands()).containsExactly("EB-01");
    }

    private ValidatableResponse unlock(String vehicleCode) {
        return given().port(port)
                .auth().preemptive().basic(username, password)
                .contentType(ContentType.JSON)
                .body("{\"vehicleCode\": \"" + vehicleCode + "\"}")
                .when().post("/api/trips")
                .then();
    }
}
