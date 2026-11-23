package pt.ua.tqs.flexmotion_cycles.acceptance;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import pt.ua.tqs.flexmotion_cycles.adapters.telemetry.FakeTelemetryAdapter;
import pt.ua.tqs.flexmotion_cycles.data.MaintenanceTicketRepository;
import pt.ua.tqs.flexmotion_cycles.data.StationRepository;
import pt.ua.tqs.flexmotion_cycles.data.TripRepository;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.MaintenanceTicket;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.StationType;
import pt.ua.tqs.flexmotion_cycles.domain.TripStatus;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleStatus;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

/** Steps for find_and_unlock_ebike.feature (FM-27): user actions go through the REST API. */
public class FindAndUnlockEbikeSteps {

    private static final double LATITUDE = 40.6405;
    private static final double LONGITUDE = -8.6538;
    private static final int DOCKED_VEHICLE_BATTERY = 80;
    private static final Instant TICKET_OPENED_AT = Instant.parse("2026-11-22T09:00:00Z");

    private final StationRepository stationRepository;
    private final VehicleRepository vehicleRepository;
    private final MaintenanceTicketRepository maintenanceTicketRepository;
    private final TripRepository tripRepository;
    private final FakeTelemetryAdapter telemetry;
    private final JdbcTemplate jdbcTemplate;
    private final int port;
    private final String username;
    private final String password;

    private final Map<String, Station> stations = new HashMap<>();
    private final Map<String, Vehicle> vehicles = new HashMap<>();
    private boolean loggedIn;
    private Response response;

    public FindAndUnlockEbikeSteps(StationRepository stationRepository,
            VehicleRepository vehicleRepository,
            MaintenanceTicketRepository maintenanceTicketRepository,
            TripRepository tripRepository, FakeTelemetryAdapter telemetry,
            JdbcTemplate jdbcTemplate,
            @Value("${local.server.port}") int port,
            @Value("${spring.security.user.name}") String username,
            @Value("${spring.security.user.password}") String password) {
        this.stationRepository = stationRepository;
        this.vehicleRepository = vehicleRepository;
        this.maintenanceTicketRepository = maintenanceTicketRepository;
        this.tripRepository = tripRepository;
        this.telemetry = telemetry;
        this.jdbcTemplate = jdbcTemplate;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    @Given("a conventional station {string} with capacity {int}")
    public void aConventionalStation(String name, int capacity) {
        stations.put(name, stationRepository.save(
                new Station(name, LATITUDE, LONGITUDE, capacity, StationType.CONVENTIONAL)));
    }

    @Given("an available {string} {string} at {string} with battery {int}%")
    public void anAvailableVehicle(String type, String code, String station, int battery) {
        vehicles.put(code, vehicleRepository.save(
                new Vehicle(code, VehicleType.valueOf(type), battery, stations.get(station))));
    }

    @Given("a {string} {string} {string} at {string} with battery {int}%")
    public void aVehicleInStatus(String status, String type, String code, String station,
            int battery) {
        anAvailableVehicle(type, code, station, battery);
        // Test fixture only: the other lifecycle transitions belong to other stories.
        jdbcTemplate.update("UPDATE vehicles SET status = ? WHERE code = ?",
                VehicleStatus.valueOf(status).name(), code);
    }

    @Given("{string} has an open maintenance ticket")
    public void hasAnOpenMaintenanceTicket(String code) {
        maintenanceTicketRepository.save(new MaintenanceTicket(vehicles.get(code), TICKET_OPENED_AT));
    }

    @Given("{int} vehicles are docked at {string}")
    public void vehiclesAreDocked(int count, String station) {
        for (int i = 1; i <= count; i++) {
            anAvailableVehicle(VehicleType.E_BIKE.name(), "DK-%02d".formatted(i), station,
                    DOCKED_VEHICLE_BATTERY);
        }
    }

    @Given("Inês is logged in")
    public void inesIsLoggedIn() {
        loggedIn = true;
    }

    @When("Inês searches for available e-bikes")
    public void inesSearchesForAvailableEbikes() {
        response = given().port(port).when().get("/api/stations");
    }

    @When("Inês unlocks {string}")
    public void inesUnlocks(String code) {
        RequestSpecification request = given().port(port)
                .contentType(ContentType.JSON)
                .body("{\"vehicleCode\": \"" + code + "\"}");
        if (loggedIn) {
            request = request.auth().preemptive().basic(username, password);
        }
        response = request.when().post("/api/trips");
    }

    @When("the telemetry reports the lock of {string} open at {string}")
    public void telemetryReportsLockOpen(String code, String openedAt) {
        telemetry.reportLockOpened(code, Instant.parse(openedAt));
    }

    @Then("{string} is listed at {string}")
    public void isListedAt(String code, String station) {
        assertThat(listedCodes(station)).contains(code);
    }

    @Then("{string} is not listed at {string}")
    public void isNotListedAt(String code, String station) {
        assertThat(listedCodes(station)).doesNotContain(code);
    }

    @Then("{string} shows {int} free docks")
    public void showsFreeDocks(String station, int freeDocks) {
        assertThat(response.jsonPath().getLong(stationPath(station) + ".freeDocks"))
                .isEqualTo(freeDocks);
    }

    @Then("an unlock command is sent for {string}")
    public void unlockCommandIsSent(String code) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.ACCEPTED.value());
        assertThat(telemetry.unlockCommands()).contains(code);
    }

    @Then("the unlock is refused")
    public void unlockIsRefused() {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Then("no unlock command is sent for {string}")
    public void noUnlockCommandIsSent(String code) {
        assertThat(telemetry.unlockCommands()).doesNotContain(code);
    }

    @Then("{string} is still {string}")
    public void isStill(String code, String status) {
        vehicleIs(code, status);
    }

    @Then("{string} is {string}")
    public void vehicleIs(String code, String status) {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM vehicles WHERE code = ?", String.class, code))
                .isEqualTo(status);
    }

    @Then("Inês has an active trip on {string} started at {string}")
    public void hasAnActiveTrip(String code, String startedAt) {
        assertThat(tripRepository.findByVehicleCodeAndStatus(code, TripStatus.ACTIVE))
                .hasValueSatisfying(trip -> {
                    assertThat(trip.getUsername()).isEqualTo(username);
                    assertThat(trip.getStartedAt()).isEqualTo(Instant.parse(startedAt));
                });
    }

    private List<String> listedCodes(String station) {
        return response.jsonPath()
                .getList(stationPath(station) + ".availableEbikes.code", String.class);
    }

    private static String stationPath(String station) {
        return "find { it.name == '" + station + "' }";
    }
}
