package pt.ua.tqs.flexmotion_cycles.services;

import org.springframework.stereotype.Service;

import pt.ua.tqs.flexmotion_cycles.data.StationRepository;
import pt.ua.tqs.flexmotion_cycles.domain.GeoPoint;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.dto.StationRequest;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.InvalidStationException;

/** Registration of stations and accessible hubs (US-03). */
@Service
public class StationService {

    /** A station needs at least one dock (US-03). */
    private static final int MIN_DOCK_CAPACITY = 1;

    /** Radius of the overflow zone of every conventional station (BR11). */
    private static final double CONVENTIONAL_OVERFLOW_RADIUS_METERS = 10.0;

    private final StationRepository stationRepository;

    public StationService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    /**
     * Registers a station after checking the US-03 rules.
     *
     * @throws InvalidStationException if a rule is broken
     */
    public Station registerStation(StationRequest request) {
        if (request.dockCapacity() < MIN_DOCK_CAPACITY) {
            throw new InvalidStationException(
                    "Dock capacity must be at least " + MIN_DOCK_CAPACITY);
        }
        GeoPoint location = new GeoPoint(request.latitude(), request.longitude());
        Station station = switch (request.type()) {
            case CONVENTIONAL -> Station.conventional(request.name(), location,
                    request.dockCapacity(), CONVENTIONAL_OVERFLOW_RADIUS_METERS);
            case ACCESSIBLE -> accessibleHub(request, location);
        };
        return stationRepository.save(station);
    }

    private static Station accessibleHub(StationRequest request, GeoPoint location) {
        if (request.stepFreeAccess() == null) {
            throw new InvalidStationException(
                    "An accessible hub must state whether it has step-free access");
        }
        if (request.technicianFrom() == null || request.technicianTo() == null) {
            throw new InvalidStationException("An accessible hub must have technician hours");
        }
        if (!request.technicianTo().isAfter(request.technicianFrom())) {
            throw new InvalidStationException("Technician hours must end after they start");
        }
        return Station.accessible(request.name(), location, request.dockCapacity(),
                request.stepFreeAccess(), request.technicianFrom(), request.technicianTo());
    }
}