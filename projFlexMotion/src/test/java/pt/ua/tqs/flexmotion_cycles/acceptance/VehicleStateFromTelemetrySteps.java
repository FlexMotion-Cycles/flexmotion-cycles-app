package pt.ua.tqs.flexmotion_cycles.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.jdbc.core.JdbcTemplate;

import pt.ua.tqs.flexmotion_cycles.adapters.telemetry.FakeTelemetryAdapter;
import pt.ua.tqs.flexmotion_cycles.data.StationRepository;
import pt.ua.tqs.flexmotion_cycles.domain.GeoPoint;
import pt.ua.tqs.flexmotion_cycles.domain.TelemetryReading;
import pt.ua.tqs.flexmotion_cycles.domain.TicketOrigin;
import pt.ua.tqs.flexmotion_cycles.domain.TicketStatus;

public class VehicleStateFromTelemetrySteps {

    private static final Instant FIRST_READING_AT = Instant.parse("2026-11-22T16:05:00Z");
    private static final Duration READING_INTERVAL = Duration.ofSeconds(5);
    private static final double METRES_PER_DEGREE_OF_LATITUDE = 111_195;

    private final FakeTelemetryAdapter telemetry;
    private final StationRepository stationRepository;
    private final JdbcTemplate jdbcTemplate;

    private Instant nextReadingAt = FIRST_READING_AT;

    public VehicleStateFromTelemetrySteps(FakeTelemetryAdapter telemetry,
            StationRepository stationRepository, JdbcTemplate jdbcTemplate) {
        this.telemetry = telemetry;
        this.stationRepository = stationRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @When("the telemetry reports {string} locked at {string} with battery {int}%")
    public void reportsLockedAtStation(String code, String station, int battery) {
        report(code, battery, locationOf(station), true, null);
    }

    @When("the telemetry reports {string} locked {int} metres away from {string}")
    public void reportsLockedAwayFromStation(String code, int metres, String station) {
        GeoPoint location = locationOf(station);
        GeoPoint away = new GeoPoint(
                location.latitude() + metres / METRES_PER_DEGREE_OF_LATITUDE,
                location.longitude());
        report(code, batteryOf(code), away, true, null);
    }

    @When("the telemetry reports the fault {string} on {string}")
    public void reportsFault(String fault, String code) {
        report(code, batteryOf(code), stationLocationOf(code), isLocked(code), fault);
    }

    @Then("{string} has battery {int}%")
    public void hasBattery(String code, int battery) {
        assertThat(batteryOf(code)).isEqualTo(battery);
    }

    @Then("{string} is locked at {string}")
    public void isLockedAt(String code, String station) {
        assertThat(isLocked(code)).isTrue();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT s.name FROM vehicles v JOIN stations s ON s.id = v.station_id
                WHERE v.code = ?""", String.class, code)).isEqualTo(station);
    }

    @Then("{string} has no station")
    public void hasNoStation(String code) {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT station_id FROM vehicles WHERE code = ?", Long.class, code)).isNull();
    }

    @Then("{string} has {int} open fault ticket")
    public void hasOpenFaultTickets(String code, int count) {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM maintenance_tickets t JOIN vehicles v ON v.id = t.vehicle_id
                WHERE v.code = ? AND t.origin = ? AND t.status = ?""", Long.class,
                code, TicketOrigin.FAULT_EVENT.name(), TicketStatus.OPEN.name()))
                .isEqualTo(count);
    }

    private void report(String code, int battery, GeoPoint position, boolean locked,
            String fault) {
        telemetry.reportReading(
                new TelemetryReading(code, nextReadingAt, battery, position, locked, fault));
        nextReadingAt = nextReadingAt.plus(READING_INTERVAL);
    }

    private GeoPoint locationOf(String station) {
        return stationRepository.findAll().stream()
                .filter(candidate -> candidate.getName().equals(station))
                .findFirst()
                .orElseThrow()
                .getLocation();
    }

    private GeoPoint stationLocationOf(String code) {
        return jdbcTemplate.queryForObject("""
                SELECT s.latitude, s.longitude FROM vehicles v
                JOIN stations s ON s.id = v.station_id WHERE v.code = ?""",
                (row, rowNumber) -> new GeoPoint(row.getDouble(1), row.getDouble(2)), code);
    }

    private int batteryOf(String code) {
        return jdbcTemplate.queryForObject(
                "SELECT battery_level FROM vehicles WHERE code = ?", Integer.class, code);
    }

    private boolean isLocked(String code) {
        return jdbcTemplate.queryForObject(
                "SELECT locked FROM vehicles WHERE code = ?", Boolean.class, code);
    }
}
