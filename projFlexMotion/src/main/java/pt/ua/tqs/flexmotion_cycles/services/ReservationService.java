package pt.ua.tqs.flexmotion_cycles.services;

import java.time.Clock;
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
        if (tripRepository.existsByUsernameAndStatusIn(username,
                ConventionalUseRules.TRIP_IN_PROGRESS)) {
            throw new ReservationRefusedException("User already has a trip in progress");
        }
        if (!reservationRepository.findByUsernameAndStatus(username, ReservationStatus.ACTIVE)
                .isEmpty()) {
            throw new ReservationRefusedException("User already has an active reservation");
        }
        if (!eligibility.isEligible(vehicle)) {
            throw new ReservationRefusedException("Vehicle " + vehicleCode + " cannot be reserved");
        }
        lifecycle.apply(vehicle, VehicleState.RESERVED);
        return reservationRepository.save(new Reservation(username, vehicle, clock.instant(),
                properties.hold(), properties.fee()));
    }
}
