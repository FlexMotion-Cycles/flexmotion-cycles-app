package pt.ua.tqs.flexmotion_cycles.services;

import org.springframework.stereotype.Component;

import pt.ua.tqs.flexmotion_cycles.data.MaintenanceTicketRepository;
import pt.ua.tqs.flexmotion_cycles.data.TripRepository;
import pt.ua.tqs.flexmotion_cycles.domain.TicketStatus;
import pt.ua.tqs.flexmotion_cycles.domain.TripStatus;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleStatus;

/** Whether a vehicle can be reserved or unlocked in the conventional service. */
@Component
public class ConventionalVehicleEligibility {

    private final MaintenanceTicketRepository maintenanceTicketRepository;
    private final TripRepository tripRepository;

    public ConventionalVehicleEligibility(MaintenanceTicketRepository maintenanceTicketRepository,
            TripRepository tripRepository) {
        this.maintenanceTicketRepository = maintenanceTicketRepository;
        this.tripRepository = tripRepository;
    }

    /** BR4 (status, battery, no open ticket), BR19 (conventional only), no pending unlock. */
    public boolean isEligible(Vehicle vehicle) {
        return vehicle.getStatus() == VehicleStatus.AVAILABLE
                && vehicle.getBatteryLevel() >= ConventionalUseRules.MIN_BATTERY_LEVEL
                && ConventionalUseRules.CONVENTIONAL_TYPES.contains(vehicle.getType())
                && !maintenanceTicketRepository.existsByVehicleAndStatus(vehicle, TicketStatus.OPEN)
                && !tripRepository.existsByVehicleAndStatus(vehicle, TripStatus.UNLOCK_REQUESTED);
    }
}
