package pt.ua.tqs.flexmotion_cycles.boundary;

import java.math.BigDecimal;
import java.time.Instant;
import pt.ua.tqs.flexmotion_cycles.domain.Reservation;
import pt.ua.tqs.flexmotion_cycles.domain.ReservationStatus;

public record ReservationDto(Long id, String vehicleCode, ReservationStatus status,
        Instant createdAt, Instant expiresAt, BigDecimal fee) {

    static ReservationDto from(Reservation reservation) {
        return new ReservationDto(reservation.getId(), reservation.getVehicle().getCode(),
                reservation.getStatus(), reservation.getCreatedAt(),
                reservation.getExpiresAt(), reservation.getFee());
    }
}
