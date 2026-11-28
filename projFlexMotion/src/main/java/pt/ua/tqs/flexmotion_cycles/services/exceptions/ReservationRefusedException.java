package pt.ua.tqs.flexmotion_cycles.services.exceptions;

/** The vehicle or the user does not meet the reservation rules (BR4, BR5). */
public class ReservationRefusedException extends RuntimeException {

    public ReservationRefusedException(String message) {
        super(message);
    }
}
