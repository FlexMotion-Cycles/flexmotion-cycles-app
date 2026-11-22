package pt.ua.tqs.flexmotion_cycles.services;

import org.springframework.stereotype.Service;

import pt.ua.tqs.flexmotion_cycles.data.StationRepository;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.dto.VehicleRequest;

/** Registration of fleet vehicles at their current station */
@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final StationRepository stationRepository;

    public VehicleService(VehicleRepository vehicleRepository,
            StationRepository stationRepository) {
        this.vehicleRepository = vehicleRepository;
        this.stationRepository = stationRepository;
    }

    /** Registers a new vehicle, AVAILABLE at its station, after checking the US-03 rules. */
    public Vehicle registerVehicle(VehicleRequest request) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}