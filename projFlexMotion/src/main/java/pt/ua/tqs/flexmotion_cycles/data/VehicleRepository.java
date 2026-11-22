package pt.ua.tqs.flexmotion_cycles.data;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

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
}
