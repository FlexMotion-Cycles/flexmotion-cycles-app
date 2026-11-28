package pt.ua.tqs.flexmotion_cycles.adapters.payment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import pt.ua.tqs.flexmotion_cycles.ports.CardLinkResult;
import pt.ua.tqs.flexmotion_cycles.ports.PaymentPort;
import pt.ua.tqs.flexmotion_cycles.ports.PaymentResult;

/**
 * In-memory stand-in for the payment sandbox, for tests and demos without network access. It
 * answers the Stripe test card tokens the same way the sandbox does.
 */
@Component
@ConditionalOnProperty(name = "flexmotion.payment.adapter", havingValue = "fake")
public class FakePaymentAdapter implements PaymentPort {


    public static final String APPROVED_CARD = "pm_card_visa";
    public static final String DECLINED_CARD = "pm_card_chargeDeclined";
    public static final String FAILING_CHARGES_CARD = "pm_card_chargeCustomerFail";

    private static final Map<String, String> LINKABLE_CARDS = Map.of(
            APPROVED_CARD, "4242",
            FAILING_CHARGES_CARD, "0341");

    private static final String REFERENCE_PREFIX = "fake_pi_";

    private final AtomicLong nextReference = new AtomicLong(1);
    private final List<String> authorizations = new CopyOnWriteArrayList<>();

    @Override
    public CardLinkResult linkCard(String cardToken) {
        String last4 = cardToken == null ? null : LINKABLE_CARDS.get(cardToken);
        return last4 == null
                ? CardLinkResult.declined()
                : CardLinkResult.approved(cardToken, last4);
    }

    @Override
    public PaymentResult authorize(String gatewayToken, BigDecimal amount) {
        if (!APPROVED_CARD.equals(gatewayToken)) {
            return PaymentResult.declined();
        }
        String reference = newReference();
        authorizations.add(reference);
        return PaymentResult.approved(reference);
    }

    @Override
    public PaymentResult capture(String gatewayRef, BigDecimal amount) {
        return closeAuthorization(gatewayRef);
    }

    @Override
    public PaymentResult release(String gatewayRef) {
        return closeAuthorization(gatewayRef);
    }

    @Override
    public PaymentResult charge(String gatewayToken, BigDecimal amount) {
        return APPROVED_CARD.equals(gatewayToken)
                ? PaymentResult.approved(newReference())
                : PaymentResult.declined();
    }

    @Override
    public PaymentResult refund(String gatewayRef, BigDecimal amount) {
        return PaymentResult.approved(gatewayRef);
    }

    public List<String> openAuthorizations() {
        return List.copyOf(authorizations);
    }

    public void reset() {
        authorizations.clear();
        nextReference.set(1);
    }

    private PaymentResult closeAuthorization(String gatewayRef) {
        return authorizations.remove(gatewayRef)
                ? PaymentResult.approved(gatewayRef)
                : PaymentResult.declined();
    }

    private String newReference() {
        return REFERENCE_PREFIX + nextReference.getAndIncrement();
    }
}
