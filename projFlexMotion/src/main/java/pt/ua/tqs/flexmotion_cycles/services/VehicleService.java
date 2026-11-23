package pt.ua.tqs.flexmotion_cycles.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ua.tqs.flexmotion_cycles.data.StationRepository;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleLifecycle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleState;
import pt.ua.tqs.flexmotion_cycles.dto.VehicleRequest;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.DuplicateVehicleCodeException;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.StationFullException;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.StationNotFoundException;

/** Registration of fleet vehicles (US-03) and their lifecycle state changes (US-04). */
@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final StationRepository stationRepository;
    private final VehicleLifecycle lifecycle = new VehicleLifecycle();

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

    /** Moves the vehicle to the given state if the lifecycle allows it (US-04). */
    @Transactional
    public Vehicle changeState(Long id, VehicleState toState) {
        Vehicle vehicle = findVehicle(id);
        lifecycle.apply(vehicle, toState);
        return vehicleRepository.save(vehicle);
    }

    /** Takes an AVAILABLE vehicle out of service: it moves to MAINTENANCE (US-04). */
    @Transactional
    public Vehicle deactivate(Long id) {
        Vehicle vehicle = findVehicle(id);
        lifecycle.deactivate(vehicle);
        return vehicleRepository.save(vehicle);
    }

    /** Puts a vehicle in MAINTENANCE back into service: it moves to AVAILABLE (US-04). */
    @Transactional
    public Vehicle activate(Long id) {
        Vehicle vehicle = findVehicle(id);
        lifecycle.activate(vehicle);
        return vehicleRepository.save(vehicle);
    }

    private Vehicle findVehicle(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException(String.valueOf(id)));
    }
}
