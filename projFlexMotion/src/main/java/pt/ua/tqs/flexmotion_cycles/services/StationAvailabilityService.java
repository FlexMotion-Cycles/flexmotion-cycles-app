package pt.ua.tqs.flexmotion_cycles.services;

import static java.util.stream.Collectors.groupingBy;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.ua.tqs.flexmotion_cycles.data.StationRepository;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.StationType;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleStatus;

@Service
@Transactional(readOnly = true)
public class StationAvailabilityService {

    /** Vehicles in these states do not occupy a dock. */
    private static final Set<VehicleStatus> NOT_DOCKED =
            EnumSet.of(VehicleStatus.IN_USE, VehicleStatus.REMOVED);

    private final StationRepository stationRepository;
    private final VehicleRepository vehicleRepository;

    public StationAvailabilityService(StationRepository stationRepository,
            VehicleRepository vehicleRepository) {
        this.stationRepository = stationRepository;
        this.vehicleRepository = vehicleRepository;
    }

    /** Conventional stations with their free docks and unlockable e-bikes (BR4, BR19). */
    public List<StationAvailability> findConventionalStations() {
        Map<Long, List<Vehicle>> availableByStation = vehicleRepository
                .findAvailableWithoutOpenTicket(ConventionalUseRules.MIN_BATTERY_LEVEL,
                        ConventionalUseRules.CONVENTIONAL_TYPES)
                .stream()
                .collect(groupingBy(vehicle -> vehicle.getStation().getId()));

        return stationRepository.findByType(StationType.CONVENTIONAL).stream()
                .map(station -> new StationAvailability(station, freeDocks(station),
                        availableByStation.getOrDefault(station.getId(), List.of())))
                .toList();
    }

    private long freeDocks(Station station) {
        return station.getCapacity()
                - vehicleRepository.countByStationAndStatusNotIn(station, NOT_DOCKED);
    }
}
