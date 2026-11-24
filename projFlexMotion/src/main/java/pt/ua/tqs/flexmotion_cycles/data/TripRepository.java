package pt.ua.tqs.flexmotion_cycles.data;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Trip;
import pt.ua.tqs.flexmotion_cycles.domain.TripStatus;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;

public interface TripRepository extends JpaRepository<Trip, Long> {

    boolean existsByVehicleAndStatus(Vehicle vehicle, TripStatus status);

    boolean existsByUsernameAndStatusIn(String username, Collection<TripStatus> statuses);

    Optional<Trip> findByVehicleCodeAndStatus(String vehicleCode, TripStatus status);

    @EntityGraph(attributePaths = {"vehicle", "endStation"})
    Optional<Trip> findByIdAndUsername(Long id, String username);
}
