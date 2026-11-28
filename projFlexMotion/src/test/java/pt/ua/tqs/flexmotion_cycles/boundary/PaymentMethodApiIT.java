package pt.ua.tqs.flexmotion_cycles.boundary;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;
import pt.ua.tqs.flexmotion_cycles.adapters.payment.FakePaymentAdapter;
import pt.ua.tqs.flexmotion_cycles.data.PaymentMethodRepository;
import pt.ua.tqs.flexmotion_cycles.data.PaymentRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("it")
class PaymentMethodApiIT {

    private static final String PAYMENT_METHODS_URL = "/api/payment-methods";

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final int port;
    private final String username;
    private final String password;

    @Autowired
    PaymentMethodApiIT(PaymentRepository paymentRepository,
            PaymentMethodRepository paymentMethodRepository,
            @Value("${local.server.port}") int port,
            @Value("${spring.security.user.name}") String username,
            @Value("${spring.security.user.password}") String password) {
        this.paymentRepository = paymentRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        paymentMethodRepository.deleteAll();
    }

    @Test
    @Requirement("FM-32")
    void anApprovedCardIsLinkedAsTheDefaultAndListed() {
        link(FakePaymentAdapter.APPROVED_CARD)
                .statusCode(201)
                .body("last4", equalTo("4242"))
                .body("isDefault", equalTo(true));

        authenticated().when().get(PAYMENT_METHODS_URL).then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].last4", equalTo("4242"));
        assertThat(paymentMethodRepository.findByUsernameAndDefaultMethodTrue(username))
                .hasValueSatisfying(card -> assertThat(card.getGatewayToken())
                        .isEqualTo(FakePaymentAdapter.APPROVED_CARD));
    }

    @Test
    @Requirement("FM-32")
    void aCardDeclinedByTheSandboxIsRefusedAndNotStored() {
        link(FakePaymentAdapter.DECLINED_CARD).statusCode(402);

        assertThat(paymentMethodRepository.existsByUsername(username)).isFalse();
    }

    @Test
    @Requirement("FM-32")
    void aSecondCardDoesNotReplaceTheDefault() {
        link(FakePaymentAdapter.APPROVED_CARD).statusCode(201);

        link(FakePaymentAdapter.FAILING_CHARGES_CARD)
                .statusCode(201)
                .body("last4", equalTo("0341"))
                .body("isDefault", equalTo(false));
    }

    private ValidatableResponse link(String cardToken) {
        return authenticated()
                .contentType(ContentType.JSON)
                .body("{\"cardToken\": \"" + cardToken + "\"}")
                .when().post(PAYMENT_METHODS_URL)
                .then();
    }

    private RequestSpecification authenticated() {
        return given().port(port).auth().preemptive().basic(username, password);
    }
}
