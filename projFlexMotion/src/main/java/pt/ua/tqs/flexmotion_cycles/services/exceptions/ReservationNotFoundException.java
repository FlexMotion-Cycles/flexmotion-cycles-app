package pt.ua.tqs.flexmotion_cycles.services.exceptions;

public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(Long reservationId) {
        super("Reservation " + reservationId + " not found");
    }
}
