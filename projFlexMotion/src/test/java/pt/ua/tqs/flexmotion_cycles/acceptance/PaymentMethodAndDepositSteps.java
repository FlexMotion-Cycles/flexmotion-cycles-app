package pt.ua.tqs.flexmotion_cycles.acceptance;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import pt.ua.tqs.flexmotion_cycles.adapters.payment.FakePaymentAdapter;
import pt.ua.tqs.flexmotion_cycles.data.PaymentRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Payment;
import pt.ua.tqs.flexmotion_cycles.domain.PaymentStatus;
import pt.ua.tqs.flexmotion_cycles.domain.PaymentType;

/** Steps of US-14 (FM-32): linking a card and the deposit held when a trip is requested. */
public class PaymentMethodAndDepositSteps {

    private static final String PAYMENT_METHODS_URL = "/api/payment-methods";

    /** Sandbox test card behind each last-four in the scenarios (same tokens as Stripe). */
    private static final Map<String, String> ACCEPTED_CARDS = Map.of(
            "4242", FakePaymentAdapter.APPROVED_CARD);
    private static final Map<String, String> DECLINED_CARDS = Map.of(
            "0002", FakePaymentAdapter.DECLINED_CARD);
    private static final Map<String, String> FAILING_DEPOSIT_CARDS = Map.of(
            "0341", FakePaymentAdapter.FAILING_CHARGES_CARD);

    private final FakePaymentAdapter payments;
    private final PaymentRepository paymentRepository;
    private final int port;
    private final String username;
    private final String password;

    private Response linkResponse;

    public PaymentMethodAndDepositSteps(FakePaymentAdapter payments,
            PaymentRepository paymentRepository,
            @Value("${local.server.port}") int port,
            @Value("${spring.security.user.name}") String username,
            @Value("${spring.security.user.password}") String password) {
        this.payments = payments;
        this.paymentRepository = paymentRepository;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    @When("Inês links a card ending in {string} that the payment sandbox accepts")
    public void inesLinksAnAcceptedCard(String last4) {
        linkResponse = link(cardToken(ACCEPTED_CARDS, last4));
    }

    @When("Inês links a card ending in {string} that the payment sandbox declines")
    public void inesLinksADeclinedCard(String last4) {
        linkResponse = link(cardToken(DECLINED_CARDS, last4));
    }

    @Given("Inês has a linked card ending in {string} that the payment sandbox accepts")
    public void inesHasALinkedAcceptedCard(String last4) {
        assertLinked(link(cardToken(ACCEPTED_CARDS, last4)));
    }

    @Given("Inês has a linked card ending in {string} whose deposit hold the payment sandbox declines")
    public void inesHasALinkedCardWhoseDepositIsDeclined(String last4) {
        assertLinked(link(cardToken(FAILING_DEPOSIT_CARDS, last4)));
    }

    @Then("Inês has a linked card ending in {string}")
    public void inesHasALinkedCardEndingIn(String last4) {
        assertThat(linkedCards()).extracting(card -> card.get("last4")).containsExactly(last4);
        assertThat(linkedCards())
                .allSatisfy(card -> assertThat(card).doesNotContainKey("gatewayToken"));
    }

    @Then("the card is refused")
    public void theCardIsRefused() {
        assertThat(linkResponse.statusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED.value());
    }

    @Then("Inês has no linked card")
    public void inesHasNoLinkedCard() {
        assertThat(linkedCards()).isEmpty();
    }

    @Then("a deposit of {bigdecimal} € is held for Inês's trip on {string}")
    public void aDepositIsHeld(BigDecimal amount, String vehicleCode) {
        Payment deposit = paymentRepository
                .findByTypeAndTripUsernameAndTripVehicleCode(PaymentType.DEPOSIT, username,
                        vehicleCode)
                .orElseThrow();
        assertThat(deposit.getAmount()).isEqualByComparingTo(amount);
        assertThat(deposit.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(payments.openAuthorizations()).containsExactly(deposit.getGatewayRef());
    }

    @Then("no deposit is held for Inês")
    public void noDepositIsHeld() {
        assertThat(paymentRepository.existsByTypeAndStatusAndTripUsername(
                PaymentType.DEPOSIT, PaymentStatus.AUTHORIZED, username)).isFalse();
        assertThat(payments.openAuthorizations()).isEmpty();
    }

    private static String cardToken(Map<String, String> cards, String last4) {
        String token = cards.get(last4);
        assertThat(token).as("sandbox test card ending in %s", last4).isNotNull();
        return token;
    }

    private Response link(String cardToken) {
        return authenticated()
                .contentType(ContentType.JSON)
                .body(Map.of("cardToken", cardToken))
                .when().post(PAYMENT_METHODS_URL);
    }

    private static void assertLinked(Response response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    private List<Map<String, Object>> linkedCards() {
        return authenticated().when().get(PAYMENT_METHODS_URL)
                .then().statusCode(HttpStatus.OK.value())
                .extract().jsonPath().getList("$");
    }

    private RequestSpecification authenticated() {
        return given().port(port).auth().preemptive().basic(username, password);
    }
}
