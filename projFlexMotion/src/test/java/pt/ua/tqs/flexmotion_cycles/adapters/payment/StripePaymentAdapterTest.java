package pt.ua.tqs.flexmotion_cycles.adapters.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.ports.CardLinkResult;
import pt.ua.tqs.flexmotion_cycles.ports.PaymentResult;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.PaymentGatewayException;

/** The Stripe adapter against a mocked Stripe API: no network and no real key. */
class StripePaymentAdapterTest {

    private static final String STRIPE = "https://stripe.test";
    private static final String TEST_KEY = "sk_test_unit_only";
    private static final BigDecimal DEPOSIT = new BigDecimal("10.00");
    private static final String CARD_DECLINED =
            "{\"error\": {\"type\": \"card_error\", \"code\": \"card_declined\"}}";

    private MockRestServiceServer server;
    private StripePaymentAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = StripePaymentConfig.stripeClient(builder,
                new StripeProperties(STRIPE, TEST_KEY));
        adapter = new StripePaymentAdapter(client, "eur");
    }

    @AfterEach
    void allExpectedCallsWereMade() {
        server.verify();
    }

    @Test
    @Requirement("FM-32")
    void linksAnApprovedCardToANewCustomerWithTheSecretKey() {
        server.expect(requestTo(STRIPE + "/customers"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_KEY))
                .andRespond(json("{\"id\": \"cus_1\"}"));
        server.expect(requestTo(STRIPE + "/payment_methods/pm_card_visa/attach"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formDataContains(Map.of("customer", "cus_1")))
                .andRespond(json("{\"id\": \"pm_1\", \"card\": {\"last4\": \"4242\"}}"));

        CardLinkResult result = adapter.linkCard("pm_card_visa");

        assertThat(result.approved()).isTrue();
        assertThat(result.gatewayToken()).isEqualTo("pm_1");
        assertThat(result.last4()).isEqualTo("4242");
    }

    @Test
    @Requirement("FM-32")
    void aCardDeclinedWhenAttachingIsNotLinked() {
        server.expect(requestTo(STRIPE + "/customers")).andRespond(json("{\"id\": \"cus_1\"}"));
        server.expect(requestTo(STRIPE + "/payment_methods/pm_card_chargeDeclined/attach"))
                .andRespond(declined());

        assertThat(adapter.linkCard("pm_card_chargeDeclined").approved()).isFalse();
    }

    @Test
    @Requirement("FM-32")
    void authorizesTheDepositInCentsWithoutCapturingIt() {
        expectCustomerOf("pm_1");
        server.expect(requestTo(STRIPE + "/payment_intents"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formDataContains(Map.of(
                        "amount", "1000",
                        "currency", "eur",
                        "customer", "cus_1",
                        "payment_method", "pm_1",
                        "capture_method", "manual",
                        "confirm", "true",
                        "automatic_payment_methods[allow_redirects]", "never")))
                .andRespond(json("{\"id\": \"pi_1\", \"status\": \"requires_capture\"}"));

        PaymentResult result = adapter.authorize("pm_1", DEPOSIT);

        assertThat(result.approved()).isTrue();
        assertThat(result.gatewayRef()).isEqualTo("pi_1");
    }

    @Test
    @Requirement("FM-32")
    void aDeclinedDepositAuthorizationIsDeclined() {
        expectCustomerOf("pm_1");
        server.expect(requestTo(STRIPE + "/payment_intents")).andRespond(declined());

        assertThat(adapter.authorize("pm_1", DEPOSIT).approved()).isFalse();
    }

    @Test
    @Requirement("FM-32")
    void capturesOnlyTheAmountDue() {
        server.expect(requestTo(STRIPE + "/payment_intents/pi_1/capture"))
                .andExpect(content().formDataContains(Map.of("amount_to_capture", "999")))
                .andRespond(json("{\"id\": \"pi_1\", \"status\": \"succeeded\"}"));

        assertThat(adapter.capture("pi_1", new BigDecimal("9.99")).approved()).isTrue();
    }

    @Test
    @Requirement("FM-32")
    void releasingCancelsTheAuthorization() {
        server.expect(requestTo(STRIPE + "/payment_intents/pi_1/cancel"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(json("{\"id\": \"pi_1\", \"status\": \"canceled\"}"));

        PaymentResult result = adapter.release("pi_1");

        assertThat(result.approved()).isTrue();
        assertThat(result.gatewayRef()).isEqualTo("pi_1");
    }

    @Test
    @Requirement("FM-32")
    void chargesWithAutomaticCapture() {
        expectCustomerOf("pm_1");
        server.expect(requestTo(STRIPE + "/payment_intents"))
                .andExpect(content().formDataContains(Map.of("capture_method", "automatic")))
                .andRespond(json("{\"id\": \"pi_2\", \"status\": \"succeeded\"}"));

        assertThat(adapter.charge("pm_1", new BigDecimal("3.75")).gatewayRef()).isEqualTo("pi_2");
    }

    @Test
    @Requirement("FM-32")
    void refundsAnAmountOfAPayment() {
        server.expect(requestTo(STRIPE + "/refunds"))
                .andExpect(content().formDataContains(
                        Map.of("payment_intent", "pi_2", "amount", "375")))
                .andRespond(json("{\"id\": \"re_1\", \"status\": \"succeeded\"}"));

        assertThat(adapter.refund("pi_2", new BigDecimal("3.75")).gatewayRef()).isEqualTo("re_1");
    }

    @Test
    @Requirement("FM-32")
    void aServerErrorIsAGatewayFailureNotADeclinedCard() {
        server.expect(requestTo(STRIPE + "/customers")).andRespond(withServerError());

        assertThatThrownBy(() -> adapter.linkCard("pm_card_visa"))
                .isInstanceOf(PaymentGatewayException.class);
    }

    @Test
    @Requirement("FM-32")
    void aRejectedKeyIsAGatewayFailureNotADeclinedCard() {
        expectCustomerOf("pm_1");
        server.expect(requestTo(STRIPE + "/payment_intents"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> adapter.authorize("pm_1", DEPOSIT))
                .isInstanceOf(PaymentGatewayException.class);
    }

    @Test
    @Requirement("FM-32")
    void anInvalidRequestKeepsStripesExplanation() {
        expectCustomerOf("pm_1");
        server.expect(requestTo(STRIPE + "/payment_intents"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\": {\"code\": \"parameter_unknown\","
                                + " \"param\": \"foo\","
                                + " \"message\": \"Received unknown parameter\"}}"));

        assertThatThrownBy(() -> adapter.authorize("pm_1", DEPOSIT))
                .isInstanceOf(PaymentGatewayException.class)
                .hasMessageContaining("400")
                .hasMessageContaining("parameter_unknown")
                .hasMessageContaining("foo");
    }

    @Test
    @Requirement("FM-32")
    void refusesFractionsOfACent() {
        assertThat(StripePaymentAdapter.cents(new BigDecimal("10.00"))).isEqualTo("1000");
        assertThatThrownBy(() -> StripePaymentAdapter.cents(new BigDecimal("9.999")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Requirement("FM-32")
    void refusesToStartWithoutASecretKey() {
        StripeProperties withoutKey = new StripeProperties(STRIPE, " ");

        assertThatThrownBy(() -> StripePaymentConfig.stripeClient(RestClient.builder(), withoutKey))
                .isInstanceOf(IllegalStateException.class);
    }

    private void expectCustomerOf(String paymentMethod) {
        server.expect(requestTo(STRIPE + "/payment_methods/" + paymentMethod))
                .andExpect(method(HttpMethod.GET))
                .andRespond(json("{\"id\": \"" + paymentMethod + "\", \"customer\": \"cus_1\"}"));
    }

    private static ResponseCreator json(String body) {
        return withSuccess(body, MediaType.APPLICATION_JSON);
    }

    private static ResponseCreator declined() {
        return withStatus(HttpStatus.PAYMENT_REQUIRED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(CARD_DECLINED);
    }
}
