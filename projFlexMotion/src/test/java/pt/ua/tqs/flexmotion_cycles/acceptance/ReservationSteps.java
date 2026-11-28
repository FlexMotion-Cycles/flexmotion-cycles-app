package pt.ua.tqs.flexmotion_cycles.acceptance;

import static io.restassured.RestAssured.given;
import static io.restassured.config.JsonConfig.jsonConfig;
import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import pt.ua.tqs.flexmotion_cycles.services.ReservationExpiryScheduler;
import pt.ua.tqs.flexmotion_cycles.services.ReservationService;

/** Steps for reserve_ebike.feature (FM-29): Inês acts through the REST API. */
public class ReservationSteps {

    private static final String OTHER_USER = "rui";

    private final MutableClock clock;
    private final ReservationService reservationService;
    private final ReservationExpiryScheduler expiryScheduler;
    private final int port;
    private final String username;
    private final String password;

    private Response response;
    private Long reservationId;

    public ReservationSteps(MutableClock clock, ReservationService reservationService,
            ReservationExpiryScheduler expiryScheduler,
            @Value("${local.server.port}") int port,
            @Value("${spring.security.user.name}") String username,
            @Value("${spring.security.user.password}") String password) {
        this.clock = clock;
        this.reservationService = reservationService;
        this.expiryScheduler = expiryScheduler;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    @Given("the time is {string}")
    public void theTimeIs(String time) {
        clock.setInstant(Instant.parse(time));
    }

    @When("Inês reserves {string}")
    public void inesReserves(String code) {
        response = given().port(port)
                .auth().preemptive().basic(username, password)
                .contentType(ContentType.JSON)
                .body("{\"vehicleCode\": \"" + code + "\"}")
                .when().post("/api/reservations");
        if (response.statusCode() == HttpStatus.CREATED.value()) {
            reservationId = response.jsonPath().getLong("id");
        }
    }

    @Given("Inês reserved {string}")
    public void inesReserved(String code) {
        inesReserves(code);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    /** Through the service: HTTP Basic only has Inês's account until US-01. */
    @Given("another user reserved {string}")
    public void anotherUserReserved(String code) {
        reservationService.reserve(OTHER_USER, code);
    }

    @When("the reservation expiry check runs")
    public void theReservationExpiryCheckRuns() {
        expiryScheduler.expireDueReservations();
    }

    @Then("the reservation is refused")
    public void theReservationIsRefused() {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Then("her reservation is {string}")
    public void herReservationIs(String status) {
        assertThat(herReservation().getString("status")).isEqualTo(status);
    }

    @Then("her reservation is {string} until {string} with a fee of {bigdecimal} €")
    public void herReservationIsUntilWithAFeeOf(String status, String expiresAt, BigDecimal fee) {
        JsonPath reservation = herReservation();
        assertThat(reservation.getString("status")).isEqualTo(status);
        assertThat(Instant.parse(reservation.getString("expiresAt")))
                .isEqualTo(Instant.parse(expiresAt));
        assertThat(reservation.<BigDecimal>get("fee")).isEqualByComparingTo(fee);
    }

    private JsonPath herReservation() {
        return given().port(port)
                .config(RestAssured.config().jsonConfig(
                        jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL)))
                .auth().preemptive().basic(username, password)
                .when().get("/api/reservations/" + reservationId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().jsonPath();
    }
}
