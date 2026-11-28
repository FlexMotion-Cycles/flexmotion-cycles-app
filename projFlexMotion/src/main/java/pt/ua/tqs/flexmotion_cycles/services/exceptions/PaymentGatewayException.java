package pt.ua.tqs.flexmotion_cycles.services.exceptions;

/**
 * The payment gateway could not answer (unreachable, rejected credentials, server error). A card
 * declined by the bank is not this: it is a normal declined result.
 */
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message) {
        super(message);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
