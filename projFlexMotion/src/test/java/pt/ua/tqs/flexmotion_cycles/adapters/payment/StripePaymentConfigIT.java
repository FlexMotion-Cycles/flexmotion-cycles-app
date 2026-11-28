package pt.ua.tqs.flexmotion_cycles.adapters.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;
import pt.ua.tqs.flexmotion_cycles.ports.PaymentPort;

/** With the stripe adapter selected the application uses Stripe; nothing is sent here. */
@SpringBootTest(properties = {
    "flexmotion.payment.adapter=stripe",
    "flexmotion.payment.stripe.secret-key=sk_test_wiring_only"
})
@Import(TestcontainersConfiguration.class)
class StripePaymentConfigIT {

    private final PaymentPort paymentPort;

    @Autowired
    StripePaymentConfigIT(PaymentPort paymentPort) {
        this.paymentPort = paymentPort;
    }

    @Test
    @Requirement("FM-32")
    void theStripeAdapterIsThePaymentPortWhenSelected() {
        assertThat(paymentPort).isInstanceOf(StripePaymentAdapter.class);
    }
}
