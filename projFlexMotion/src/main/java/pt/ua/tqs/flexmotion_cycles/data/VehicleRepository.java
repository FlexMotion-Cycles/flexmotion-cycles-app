package pt.ua.tqs.flexmotion_cycles.data;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleStatus;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    /** Whether a vehicle with this identifier code is already registered. */
    boolean existsByCode(String code);

    /** Number of vehicles currently docked at the given station. */
    long countByStationId(Long stationId);

    /**
     * AVAILABLE vehicles of the given types, with at least the given battery level
     * and no OPEN maintenance ticket.
     */
    @Query("""
            SELECT v FROM Vehicle v
            WHERE v.status = pt.ua.tqs.flexmotion_cycles.domain.VehicleStatus.AVAILABLE
              AND v.batteryLevel >= :minBatteryLevel
              AND v.type IN :types
              AND NOT EXISTS (
                  SELECT t FROM MaintenanceTicket t
                  WHERE t.vehicle = v
                    AND t.status = pt.ua.tqs.flexmotion_cycles.domain.TicketStatus.OPEN)
            """)
    List<Vehicle> findAvailableWithoutOpenTicket(
            @Param("minBatteryLevel") int minBatteryLevel,
            @Param("types") Collection<VehicleType> types);

    long countByStationAndStatusNotIn(Station station, Collection<VehicleStatus> statuses);

    /**
     * Loads the vehicle and bumps its version on commit, so two concurrent unlocks of the
     * same vehicle cannot both succeed (optimistic locking).
     */
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    Optional<Vehicle> findLockedByCode(String code);
}