package pt.ua.tqs.flexmotion_cycles.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;

@Repository 
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    
}
