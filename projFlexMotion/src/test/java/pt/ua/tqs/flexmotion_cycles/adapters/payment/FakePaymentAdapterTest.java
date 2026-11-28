package pt.ua.tqs.flexmotion_cycles.adapters.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.ports.CardLinkResult;
import pt.ua.tqs.flexmotion_cycles.ports.PaymentResult;

class FakePaymentAdapterTest {

    private static final BigDecimal DEPOSIT = new BigDecimal("10.00");

    private FakePaymentAdapter payments;

    @BeforeEach
    void setUp() {
        payments = new FakePaymentAdapter();
    }

    @Test
    @Requirement("FM-32")
    void linksAnApprovedCardWithItsLastFourDigits() {
        CardLinkResult result = payments.linkCard(FakePaymentAdapter.APPROVED_CARD);

        assertThat(result.approved()).isTrue();
        assertThat(result.gatewayToken()).isEqualTo(FakePaymentAdapter.APPROVED_CARD);
        assertThat(result.last4()).isEqualTo("4242");
    }

    @Test
    @Requirement("FM-32")
    void declinesLinkingADeclinedCard() {
        CardLinkResult result = payments.linkCard(FakePaymentAdapter.DECLINED_CARD);

        assertThat(result.approved()).isFalse();
        assertThat(result.gatewayToken()).isNull();
    }

    @Test
    @Requirement("FM-32")
    void declinesLinkingAnUnknownOrMissingCard() {
        assertThat(payments.linkCard("pm_unknown").approved()).isFalse();
        assertThat(payments.linkCard(null).approved()).isFalse();
    }

    @Test
    @Requirement("FM-32")
    void linksACardWhoseChargesFail() {
        CardLinkResult result = payments.linkCard(FakePaymentAdapter.FAILING_CHARGES_CARD);

        assertThat(result.approved()).isTrue();
        assertThat(result.last4()).isEqualTo("0341");
    }

    @Test
    @Requirement("FM-32")
    void authorizesTheDepositOnAnApprovedCard() {
        PaymentResult result = payments.authorize(FakePaymentAdapter.APPROVED_CARD, DEPOSIT);

        assertThat(result.approved()).isTrue();
        assertThat(result.gatewayRef()).isNotBlank();
        assertThat(payments.openAuthorizations()).containsExactly(result.gatewayRef());
    }

    @Test
    @Requirement("FM-32")
    void declinesTheDepositAuthorizationOnACardWhoseChargesFail() {
        PaymentResult result = payments.authorize(FakePaymentAdapter.FAILING_CHARGES_CARD, DEPOSIT);

        assertThat(result.approved()).isFalse();
        assertThat(payments.openAuthorizations()).isEmpty();
    }

    @Test
    @Requirement("FM-32")
    void releasingAnAuthorizationClosesItOnlyOnce() {
        String reference =
                payments.authorize(FakePaymentAdapter.APPROVED_CARD, DEPOSIT).gatewayRef();

        assertThat(payments.release(reference).approved()).isTrue();
        assertThat(payments.release(reference).approved()).isFalse();
        assertThat(payments.openAuthorizations()).isEmpty();
    }

    @Test
    @Requirement("FM-32")
    void capturingAnAuthorizationClosesIt() {
        String reference =
                payments.authorize(FakePaymentAdapter.APPROVED_CARD, DEPOSIT).gatewayRef();

        assertThat(payments.capture(reference, new BigDecimal("9.99")).approved()).isTrue();
        assertThat(payments.openAuthorizations()).isEmpty();
    }

    @Test
    @Requirement("FM-32")
    void chargesOnlyAnApprovedCard() {
        assertThat(payments.charge(FakePaymentAdapter.APPROVED_CARD, DEPOSIT).approved()).isTrue();
        assertThat(payments.charge(FakePaymentAdapter.FAILING_CHARGES_CARD, DEPOSIT).approved())
                .isFalse();
    }

    @Test
    @Requirement("FM-32")
    void eachAuthorizationGetsItsOwnReference() {
        String first = payments.authorize(FakePaymentAdapter.APPROVED_CARD, DEPOSIT).gatewayRef();
        String second = payments.authorize(FakePaymentAdapter.APPROVED_CARD, DEPOSIT).gatewayRef();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @Requirement("FM-32")
    void resetForgetsOpenAuthorizations() {
        payments.authorize(FakePaymentAdapter.APPROVED_CARD, DEPOSIT);

        payments.reset();

        assertThat(payments.openAuthorizations()).isEmpty();
    }
}
