package pt.ua.tqs.flexmotion_cycles.ports;

/** Gateway answer to a money operation, with the gateway reference when approved. */
public record PaymentResult(boolean approved, String gatewayRef) {

    public static PaymentResult approved(String gatewayRef) {
        return new PaymentResult(true, gatewayRef);
    }

    public static PaymentResult declined() {
        return new PaymentResult(false, null);
    }
}
