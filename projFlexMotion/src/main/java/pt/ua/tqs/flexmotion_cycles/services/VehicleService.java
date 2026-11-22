package pt.ua.tqs.flexmotion_cycles.services;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleLifecycle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleState;

@Service 
public class VehicleService {
    private final VehicleRepository repository;
    private final VehicleLifecycle lifecycle = new VehicleLifecycle();

    public VehicleService(VehicleRepository repository) {
        this.repository = repository;
    }

    @Transactional 
    public Vehicle changeState(Long id, VehicleState toState) {
        throw new UnsupportedOperationException("Not implemented yet");
    } 

    @Transactional 
    public Vehicle deactivate(Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Transactional 
    public Vehicle activate(Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
