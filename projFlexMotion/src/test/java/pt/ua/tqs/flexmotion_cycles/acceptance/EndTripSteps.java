package pt.ua.tqs.flexmotion_cycles.acceptance;

import static io.restassured.RestAssured.given;
import static io.restassured.config.JsonConfig.jsonConfig;
import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.path.json.config.JsonPathConfig;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import pt.ua.tqs.flexmotion_cycles.adapters.telemetry.FakeTelemetryAdapter;
import pt.ua.tqs.flexmotion_cycles.data.StationRepository;
import pt.ua.tqs.flexmotion_cycles.domain.GeoPoint;
import pt.ua.tqs.flexmotion_cycles.domain.Station;


public class EndTripSteps {
    /** Metres in one degree of latitude, on the same Earth radius as GeoPoint (6 371 km). */
    private static final double METRES_PER_DEGREE_OF_LATITUDE = 111_195;

    private final StationRepository stationRepository;
    private final FakeTelemetryAdapter telemetry;
    private final JdbcTemplate jdbcTemplate;
    private final int port;
    private final String username;
    private final String password;

    public EndTripSteps(StationRepository stationRepository, FakeTelemetryAdapter telemetry,
            JdbcTemplate jdbcTemplate,
            @Value("${local.server.port}") int port,
            @Value("${spring.security.user.name}") String username,
            @Value("${spring.security.user.password}") String password) {
        this.stationRepository = stationRepository;
        this.telemetry = telemetry;
        this.jdbcTemplate = jdbcTemplate;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    @When("the telemetry reports the lock of {string} closed at {string} at {string}")
    public void telemetryReportsLockClosedAtStation(String code, String station, String closedAt) {
        telemetry.reportLockClosed(code, Instant.parse(closedAt), locationOf(station));
    }

    @When("the telemetry reports the lock of {string} closed {int} m away from {string} at {string}")
    public void telemetryReportsLockClosedAwayFromStation(String code, int metres, String station,
            String closedAt) {
        GeoPoint dock = locationOf(station);
        GeoPoint north = new GeoPoint(
                dock.latitude() + metres / METRES_PER_DEGREE_OF_LATITUDE, dock.longitude());
        telemetry.reportLockClosed(code, Instant.parse(closedAt), north);
    }

    @Then("Inês's trip on {string} is {string}")
    public void tripIs(String code, String status) {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT t.status FROM trips t JOIN vehicles v ON v.id = t.vehicle_id
                WHERE v.code = ?
                """, String.class, code))
                .isEqualTo(status);
    }

    @Then("{string} is {string} at {string}")
    public void vehicleIsAt(String code, String status, String station) {
        Map<String, Object> vehicle = jdbcTemplate.queryForMap("""
                SELECT v.status, s.name FROM vehicles v LEFT JOIN stations s ON s.id = v.station_id
                WHERE v.code = ?
                """, code);
        assertThat(vehicle.get("status")).isEqualTo(status);
        assertThat(vehicle.get("name")).isEqualTo(station);
    }

    @Then("Inês sees a trip cost on {string} of {bigdecimal} € for {int} minutes with {int} overtime minutes")
    public void inesSeesTripCost(String code, BigDecimal total, int minutes, int overtimeMinutes) {
        JsonPath trip = given().port(port)
                .config(RestAssured.config().jsonConfig(
                        jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL)))
                .auth().preemptive().basic(username, password)
                .when().get("/api/trips/" + tripIdOf(code))
                .then().statusCode(HttpStatus.OK.value())
                .extract().jsonPath();

        assertThat(trip.getString("status")).isEqualTo("ENDED");
        assertThat(trip.getInt("cost.billedMinutes")).isEqualTo(minutes);
        assertThat(trip.getInt("cost.overtimeMinutes")).isEqualTo(overtimeMinutes);
        assertThat(trip.<BigDecimal>get("cost.total")).isEqualByComparingTo(total);
    }

    private GeoPoint locationOf(String station) {
        return stationRepository.findAll().stream()
                .filter(candidate -> candidate.getName().equals(station))
                .map(Station::getLocation)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No station " + station));
    }

    private Long tripIdOf(String code) {
        return jdbcTemplate.queryForObject("""
                SELECT t.id FROM trips t JOIN vehicles v ON v.id = t.vehicle_id WHERE v.code = ?
                """, Long.class, code);
    }
}
