package pt.ua.tqs.flexmotion_cycles.services.exceptions;

/** BR2: unlocking or reserving needs a linked payment method. */
public class PaymentMethodRequiredException extends RuntimeException {

    public PaymentMethodRequiredException() {
        super("A linked payment method is required");
    }
}
