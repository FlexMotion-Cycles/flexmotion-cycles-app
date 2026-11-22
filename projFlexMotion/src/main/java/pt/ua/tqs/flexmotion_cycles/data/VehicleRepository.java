package pt.ua.tqs.flexmotion_cycles.data;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;

/** Persistence of fleet vehicles. */
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    /** Whether a vehicle with this identifier code is already registered. */
    boolean existsByCode(String code);

    /** Number of vehicles currently docked at the given station. */
    long countByStationId(Long stationId);
}