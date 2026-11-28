package pt.ua.tqs.flexmotion_cycles.domain;

/** Reservation states (docs/umlflexmotion.drawio.png). CANCELLED is kept for cancellation and BR23. */
public enum ReservationStatus {
    ACTIVE,
    FULFILLED,
    EXPIRED,
    CANCELLED
}
