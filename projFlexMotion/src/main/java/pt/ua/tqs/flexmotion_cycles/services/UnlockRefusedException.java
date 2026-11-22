package pt.ua.tqs.flexmotion_cycles.services;

/** The vehicle or the user does not meet the unlock rules. */
public class UnlockRefusedException extends RuntimeException {

    public UnlockRefusedException(String message) {
        super(message);
    }
}
