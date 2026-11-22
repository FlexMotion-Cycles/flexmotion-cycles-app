package pt.ua.tqs.flexmotion_cycles.services;

import java.util.Optional;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleLifecycle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleState;
import pt.ua.tqs.flexmotion_cycles.domain.exceptions.VehicleNotFoundException;

@Service 
public class VehicleService {
    private final VehicleRepository repository;
    private final VehicleLifecycle lifecycle = new VehicleLifecycle();

    public VehicleService(VehicleRepository repository) {
        this.repository = repository;
    }

    @Transactional 
    public Vehicle changeState(Long id, VehicleState toState) {
        Optional<Vehicle> vehicleopt = repository.findById(id);
        if (vehicleopt.isEmpty()) {
            throw new VehicleNotFoundException(id);
        }
        Vehicle vehicle = vehicleopt.get();
        lifecycle.apply(vehicle, toState);
        return repository.save(vehicle);
    } 

    @Transactional 
    public Vehicle deactivate(Long id) {
        Optional<Vehicle> vehicleopt = repository.findById(id);
        if (vehicleopt.isEmpty()) {
            throw new VehicleNotFoundException(id);
        }
        Vehicle vehicle = vehicleopt.get();
        lifecycle.deactivate(vehicle);
        return repository.save(vehicle);
    }

    @Transactional 
    public Vehicle activate(Long id) {
        Optional<Vehicle> vehicleopt = repository.findById(id);
        if (vehicleopt.isEmpty()) {
            throw new VehicleNotFoundException(id);
        }
        Vehicle vehicle = vehicleopt.get();
        lifecycle.activate(vehicle);
        return repository.save(vehicle);
    }
}
