package pt.ua.tqs.flexmotion_cycles.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ua.tqs.flexmotion_cycles.data.StationRepository;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.dto.VehicleRequest;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.DuplicateVehicleCodeException;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.StationFullException;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.StationNotFoundException;

/** Registration of fleet vehicles at their current station (US-03). */
@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final StationRepository stationRepository;

    public VehicleService(VehicleRepository vehicleRepository,
            StationRepository stationRepository) {
        this.vehicleRepository = vehicleRepository;
        this.stationRepository = stationRepository;
    }

    /**
     * Registers a new vehicle, AVAILABLE at its station, after checking the US-03 rules.
     *
     * @throws DuplicateVehicleCodeException if the code is already registered
     * @throws StationNotFoundException if the station does not exist
     * @throws StationFullException if the station has no free dock
     */
    @Transactional
    public Vehicle registerVehicle(VehicleRequest request) {
        if (vehicleRepository.existsByCode(request.code())) {
            throw new DuplicateVehicleCodeException(request.code());
        }
        Station station = stationRepository.findById(request.stationId())
                .orElseThrow(() -> new StationNotFoundException(request.stationId()));
        long docked = vehicleRepository.countByStationId(station.getId());
        if (docked >= station.getDockCapacity()) {
            throw new StationFullException(station.getId());
        }
        return vehicleRepository.save(new Vehicle(request.code(), request.type(), station));
    }
}