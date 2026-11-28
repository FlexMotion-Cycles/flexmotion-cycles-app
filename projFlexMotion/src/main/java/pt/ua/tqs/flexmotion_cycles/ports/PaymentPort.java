package pt.ua.tqs.flexmotion_cycles.ports;

import java.math.BigDecimal;

/**
 * Outbound port to the payment gateway (sandbox only). Covers linking a card, the deposit
 * authorization (the "hold" of BR16) and its capture or release, the charge (BR8) and the
 * refund. Amounts are in the configured currency.
 */
public interface PaymentPort {

    /** Verifies a card token from the client; a declined card must not be linked (BR2). */
    CardLinkResult linkCard(String cardToken);

    /** Reserves the amount on the card without charging it; approved leaves it AUTHORIZED. */
    PaymentResult authorize(String gatewayToken, BigDecimal amount);

    /** Charges up to the authorized amount; any remainder is freed. */
    PaymentResult capture(String gatewayRef, BigDecimal amount);

    /** Frees an authorization without charging it. */
    PaymentResult release(String gatewayRef);

    /** Charges the card directly. */
    PaymentResult charge(String gatewayToken, BigDecimal amount);

    /** Gives back an amount of a charged payment. */
    PaymentResult refund(String gatewayRef, BigDecimal amount);
}
