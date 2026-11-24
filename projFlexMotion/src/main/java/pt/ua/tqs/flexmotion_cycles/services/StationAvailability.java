package pt.ua.tqs.flexmotion_cycles.services;

import java.util.List;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;

/** Read model: a station with its free docks and the e-bikes a user can unlock there. */
public record StationAvailability(Station station, long freeDocks,
        List<Vehicle> availableVehicles) {
}
