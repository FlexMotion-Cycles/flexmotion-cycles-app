package pt.ua.tqs.flexmotion_cycles.services;

import java.time.Clock;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.ua.tqs.flexmotion_cycles.data.MaintenanceTicketRepository;
import pt.ua.tqs.flexmotion_cycles.data.TripRepository;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.TicketStatus;
import pt.ua.tqs.flexmotion_cycles.domain.Trip;
import pt.ua.tqs.flexmotion_cycles.domain.TripStatus;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleStatus;
import pt.ua.tqs.flexmotion_cycles.ports.LockOpenedEvent;
import pt.ua.tqs.flexmotion_cycles.ports.TelemetryPort;

@Service
public class TripService {

    /** A user may have only one trip requested or active at a time. */
    private static final Set<TripStatus> TRIP_IN_PROGRESS =
            EnumSet.of(TripStatus.UNLOCK_REQUESTED, TripStatus.ACTIVE);

    private final VehicleRepository vehicleRepository;
    private final MaintenanceTicketRepository maintenanceTicketRepository;
    private final TripRepository tripRepository;
    private final TelemetryPort telemetryPort;
    private final Clock clock;

    public TripService(VehicleRepository vehicleRepository,
            MaintenanceTicketRepository maintenanceTicketRepository,
            TripRepository tripRepository, TelemetryPort telemetryPort, Clock clock) {
        this.vehicleRepository = vehicleRepository;
        this.maintenanceTicketRepository = maintenanceTicketRepository;
        this.tripRepository = tripRepository;
        this.telemetryPort = telemetryPort;
        this.clock = clock;
    }

    /**
     * Unlock without a reservation (immediate use): checks the rules, records the request and
     * sends the unlock command. The trip only starts when the lock opens (BR7).
     */
    @Transactional
    public Trip requestUnlock(String username, String vehicleCode) {
        Vehicle vehicle = vehicleRepository.findLockedByCode(vehicleCode)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleCode));
        if (tripRepository.existsByUsernameAndStatusIn(username, TRIP_IN_PROGRESS)) {
            throw new UnlockRefusedException("User already has a trip in progress");
        }
        if (!isUnlockable(vehicle)) {
            throw new UnlockRefusedException("Vehicle " + vehicleCode + " cannot be unlocked");
        }
        Trip trip = tripRepository.save(new Trip(username, vehicle, clock.instant()));
        telemetryPort.sendUnlockCommand(vehicleCode);
        return trip;
    }

    /** Telemetry: the lock opened, so the vehicle is IN_USE and the trip starts (BR7). */
    @EventListener
    @Transactional
    public void onLockOpened(LockOpenedEvent event) {
        // A lock opening without a pending unlock (e.g. by a technician) is not a trip.
        tripRepository
                .findByVehicleCodeAndStatus(event.vehicleCode(), TripStatus.UNLOCK_REQUESTED)
                .ifPresent(trip -> {
                    trip.getVehicle().startUse();
                    trip.start(event.openedAt());
                });
    }

    /** BR4 (status, battery, no open ticket), BR19 (conventional only), no pending unlock. */
    private boolean isUnlockable(Vehicle vehicle) {
        return vehicle.getStatus() == VehicleStatus.AVAILABLE
                && vehicle.getBatteryLevel() >= ConventionalUseRules.MIN_BATTERY_LEVEL
                && ConventionalUseRules.CONVENTIONAL_TYPES.contains(vehicle.getType())
                && !maintenanceTicketRepository.existsByVehicleAndStatus(vehicle, TicketStatus.OPEN)
                && !tripRepository.existsByVehicleAndStatus(vehicle, TripStatus.UNLOCK_REQUESTED);
    }
}
