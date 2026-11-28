package pt.ua.tqs.flexmotion_cycles.services;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ua.tqs.flexmotion_cycles.config.ReservationProperties;
import pt.ua.tqs.flexmotion_cycles.data.ReservationRepository;
import pt.ua.tqs.flexmotion_cycles.data.TripRepository;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Reservation;
import pt.ua.tqs.flexmotion_cycles.domain.ReservationStatus;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleLifecycle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleState;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.ReservationNotFoundException;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.ReservationRefusedException;

/** Reservations of conventional vehicles (US-11). */
@Service
public class ReservationService {

    private final VehicleRepository vehicleRepository;
    private final ReservationRepository reservationRepository;
    private final TripRepository tripRepository;
    private final ConventionalVehicleEligibility eligibility;
    private final ReservationProperties properties;
    private final Clock clock;
    private final VehicleLifecycle lifecycle = new VehicleLifecycle();

    public ReservationService(VehicleRepository vehicleRepository,
            ReservationRepository reservationRepository, TripRepository tripRepository,
            ConventionalVehicleEligibility eligibility, ReservationProperties properties,
            Clock clock) {
        this.vehicleRepository = vehicleRepository;
        this.reservationRepository = reservationRepository;
        this.tripRepository = tripRepository;
        this.eligibility = eligibility;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Holds the vehicle exclusively for the user (BR5) for a fixed fee (BR6). The vehicle is
     * loaded with optimistic locking, so of two concurrent reservations only one commits.
     */
    @Transactional
    public Reservation reserve(String username, String vehicleCode) {
        Vehicle vehicle = vehicleRepository.findLockedByCode(vehicleCode)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleCode));
        Instant now = clock.instant();
        // Due reservations the expiry job has not reached yet must not block this one (BR5).
        reservationRepository.findByVehicleAndStatus(vehicle, ReservationStatus.ACTIVE)
                .ifPresent(reservation -> expireWhenDue(reservation, now));
        List<Reservation> userReservations =
                reservationRepository.findByUsernameAndStatus(username, ReservationStatus.ACTIVE);
        userReservations.forEach(reservation -> expireWhenDue(reservation, now));

        if (tripRepository.existsByUsernameAndStatusIn(username,
                ConventionalUseRules.TRIP_IN_PROGRESS)) {
            throw new ReservationRefusedException("User already has a trip in progress");
        }
        if (userReservations.stream()
                .anyMatch(reservation -> reservation.getStatus() == ReservationStatus.ACTIVE)) {
            throw new ReservationRefusedException("User already has an active reservation");
        }
        if (!eligibility.isEligible(vehicle)) {
            throw new ReservationRefusedException("Vehicle " + vehicleCode + " cannot be reserved");
        }
        lifecycle.apply(vehicle, VehicleState.RESERVED);
        return reservationRepository.save(
                new Reservation(username, vehicle, now, properties.hold(), properties.fee()));
    }

    /** The user's own reservation; another user's is not found, like an unknown one. */
    @Transactional(readOnly = true)
    public Reservation findReservation(String username, Long reservationId) {
        return reservationRepository.findByIdAndUsername(reservationId, username)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
    }

    /** Ids of the ACTIVE reservations whose hold has ended, for the expiry job (BR5). */
    @Transactional(readOnly = true)
    public List<Long> findDueReservationIds() {
        return reservationRepository
                .findByStatusAndExpiresAtLessThanEqual(ReservationStatus.ACTIVE, clock.instant())
                .stream()
                .map(Reservation::getId)
                .toList();
    }

    /**
     * BR5: expires the reservation if its hold has ended, and the vehicle is AVAILABLE again.
     * The fee is still due (BR6). Returns false when it is unknown, not ACTIVE or not due yet.
     */
    @Transactional
    public boolean expireIfDue(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .map(reservation -> expireWhenDue(reservation, clock.instant()))
                .orElse(false);
    }

    /**
     * Unlock of a vehicle (BR5). A reservation whose hold has ended is expired first, so the
     * vehicle is used immediately. The owner's reservation is fulfilled; another user is refused.
     * Returns the fulfilled reservation, or empty when the vehicle is not reserved.
     */
    @Transactional
    public Optional<Reservation> fulfilForUnlock(String username, Vehicle vehicle, Instant now) {
        Optional<Reservation> holding = reservationRepository
                .findByVehicleAndStatus(vehicle, ReservationStatus.ACTIVE)
                .filter(reservation -> !expireWhenDue(reservation, now));
        holding.ifPresent(reservation -> {
            if (!reservation.getUsername().equals(username)) {
                throw new UnlockRefusedException(
                        "Vehicle " + vehicle.getCode() + " is reserved by another user");
            }
            if (!eligibility.isEligibleWhenReserved(vehicle)) {
                throw new UnlockRefusedException(
                        "Vehicle " + vehicle.getCode() + " cannot be unlocked");
            }
            reservation.fulfil(now);
        });
        return holding;
    }

    private boolean expireWhenDue(Reservation reservation, Instant now) {
        if (reservation.getStatus() != ReservationStatus.ACTIVE || !reservation.isDueAt(now)) {
            return false;
        }
        reservation.expire(now);
        lifecycle.apply(reservation.getVehicle(), VehicleState.AVAILABLE);
        return true;
    }
}
