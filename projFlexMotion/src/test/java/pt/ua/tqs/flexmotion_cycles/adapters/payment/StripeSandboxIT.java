package pt.ua.tqs.flexmotion_cycles.adapters.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;
import pt.ua.tqs.flexmotion_cycles.ports.CardLinkResult;
import pt.ua.tqs.flexmotion_cycles.ports.PaymentResult;

/**
 * The Stripe adapter against the real Stripe test mode (sandbox, no real money). Runs only when a
 * test secret key is in STRIPE_SECRET_KEY, so it is skipped where no key is configured (e.g. CI).
 */
@SpringBootTest(properties = "flexmotion.payment.adapter=stripe")
@Import(TestcontainersConfiguration.class)
@EnabledIfEnvironmentVariable(named = "STRIPE_SECRET_KEY", matches = "sk_test_.+")
class StripeSandboxIT {

    private static final BigDecimal DEPOSIT = new BigDecimal("10.00");

    private final StripePaymentAdapter stripe;

    @Autowired
    StripeSandboxIT(StripePaymentAdapter stripe) {
        this.stripe = stripe;
    }

    @Test
    @Requirement("FM-32")
    void linksAnApprovedTestCardAndAuthorizesAndReleasesTheDeposit() {
        CardLinkResult card = stripe.linkCard(FakePaymentAdapter.APPROVED_CARD);
        assertThat(card.approved()).isTrue();
        assertThat(card.last4()).isEqualTo("4242");

        PaymentResult deposit = stripe.authorize(card.gatewayToken(), DEPOSIT);
        assertThat(deposit.approved()).isTrue();

        assertThat(stripe.release(deposit.gatewayRef()).approved()).isTrue();
    }

    @Test
    @Requirement("FM-32")
    void theSandboxDeclinesLinkingADeclinedTestCard() {
        assertThat(stripe.linkCard(FakePaymentAdapter.DECLINED_CARD).approved()).isFalse();
    }

    @Test
    @Requirement("FM-32")
    void theSandboxDeclinesTheDepositOnACardWhoseChargesFail() {
        CardLinkResult card = stripe.linkCard(FakePaymentAdapter.FAILING_CHARGES_CARD);
        assertThat(card.approved()).isTrue();

        assertThat(stripe.authorize(card.gatewayToken(), DEPOSIT).approved()).isFalse();
    }
}
