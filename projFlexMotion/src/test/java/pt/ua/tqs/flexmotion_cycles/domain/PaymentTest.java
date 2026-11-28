package pt.ua.tqs.flexmotion_cycles.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

class PaymentTest {

    private static final BigDecimal DEPOSIT = new BigDecimal("10.00");
    private static final Instant CREATED_AT = Instant.parse("2026-11-22T10:00:00Z");

    private PaymentMethod card;
    private Trip trip;

    @BeforeEach
    void setUp() {
        card = new PaymentMethod("ines", "pm_token_123", "4242");
        Station central = Station.conventional("Central", new GeoPoint(40.6405, -8.6538), 10, 10.0);
        trip = new Trip("ines", new Vehicle("EB-01", VehicleType.E_BIKE, central), CREATED_AT);
    }

    private Payment deposit() {
        return new Payment(PaymentType.DEPOSIT, DEPOSIT, card, trip, CREATED_AT);
    }

    @Test
    @Requirement("FM-32")
    void aNewPaymentIsPendingOnTheCardAndTrip() {
        Payment payment = deposit();

        assertThat(payment.getType()).isEqualTo(PaymentType.DEPOSIT);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getAmount()).isEqualByComparingTo("10.00");
        assertThat(payment.getPaymentMethod()).isSameAs(card);
        assertThat(payment.getTrip()).isSameAs(trip);
        assertThat(payment.getGatewayRef()).isNull();
        assertThat(payment.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @Requirement("FM-32")
    void anAcceptedDepositHoldIsAuthorizedWithTheGatewayReference() {
        Payment payment = deposit();

        payment.markAuthorized("pi_123");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(payment.getGatewayRef()).isEqualTo("pi_123");
    }

    @Test
    @Requirement("FM-32")
    void aDeclinedDepositHoldIsDeclined() {
        Payment payment = deposit();

        payment.markDeclined();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DECLINED);
    }

    @Test
    @Requirement("FM-32")
    void anAuthorizedHoldCanBeReleasedOrCaptured() {
        Payment released = deposit();
        released.markAuthorized("pi_1");
        released.markReleased();

        Payment captured = deposit();
        captured.markAuthorized("pi_2");
        captured.markCaptured("pi_2");

        assertThat(released.getStatus()).isEqualTo(PaymentStatus.RELEASED);
        assertThat(captured.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
    }

    @Test
    @Requirement("FM-32")
    void aCapturedPaymentCanBeRefunded() {
        Payment payment = deposit();
        payment.markCaptured("pi_123");

        payment.markRefunded();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @ParameterizedTest(name = "{0} cannot be {1}")
    @Requirement("FM-32")
    @CsvSource({
        "PENDING, RELEASED",
        "PENDING, REFUNDED",
        "AUTHORIZED, DECLINED",
        "AUTHORIZED, REFUNDED",
        "DECLINED, AUTHORIZED",
        "RELEASED, CAPTURED",
        "REFUNDED, CAPTURED"
    })
    void rejectsInvalidTransitions(PaymentStatus from, PaymentStatus to) {
        Payment payment = paymentIn(from);

        assertThatThrownBy(() -> moveTo(payment, to)).isInstanceOf(IllegalStateException.class);
        assertThat(payment.getStatus()).isEqualTo(from);
    }

    @ParameterizedTest
    @Requirement("FM-32")
    @ValueSource(strings = {"0.00", "-0.01"})
    void refusesAnAmountThatIsNotPositive(String amount) {
        BigDecimal value = new BigDecimal(amount);

        assertThatThrownBy(() -> new Payment(PaymentType.DEPOSIT, value, card, trip, CREATED_AT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Requirement("FM-32")
    void refusesAPaymentWithoutACard() {
        assertThatThrownBy(() -> new Payment(PaymentType.DEPOSIT, DEPOSIT, null, trip, CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @Requirement("FM-32")
    void refusesAnAuthorizationWithoutAGatewayReference() {
        Payment payment = deposit();

        assertThatThrownBy(() -> payment.markAuthorized(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    private Payment paymentIn(PaymentStatus status) {
        Payment payment = deposit();
        switch (status) {
            case PENDING -> { }
            case AUTHORIZED -> payment.markAuthorized("pi_1");
            case DECLINED -> payment.markDeclined();
            case CAPTURED -> payment.markCaptured("pi_1");
            case RELEASED -> {
                payment.markAuthorized("pi_1");
                payment.markReleased();
            }
            case REFUNDED -> {
                payment.markCaptured("pi_1");
                payment.markRefunded();
            }
        }
        return payment;
    }

    private static void moveTo(Payment payment, PaymentStatus status) {
        switch (status) {
            case AUTHORIZED -> payment.markAuthorized("pi_2");
            case CAPTURED -> payment.markCaptured("pi_2");
            case DECLINED -> payment.markDeclined();
            case RELEASED -> payment.markReleased();
            case REFUNDED -> payment.markRefunded();
            case PENDING -> throw new IllegalArgumentException("No transition back to PENDING");
        }
    }
}
