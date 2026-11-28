package pt.ua.tqs.flexmotion_cycles.data;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Reservation;
import pt.ua.tqs.flexmotion_cycles.domain.ReservationStatus;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /** The reservation holding the vehicle; at most one is ACTIVE thanks to the vehicle lock (BR5). */
    Optional<Reservation> findByVehicleAndStatus(Vehicle vehicle, ReservationStatus status);

    /** A user's reservations in a status; a list because two concurrent requests may create two. */
    List<Reservation> findByUsernameAndStatus(String username, ReservationStatus status);

    /** Reservations whose hold has ended at {@code now} (BR5: expiresAt == now is already due). */
    List<Reservation> findByStatusAndExpiresAtLessThanEqual(ReservationStatus status, Instant now);

    /** A reservation of its owner only, with the vehicle loaded for the response DTO. */
    @EntityGraph(attributePaths = "vehicle")
    Optional<Reservation> findByIdAndUsername(Long id, String username);
}
