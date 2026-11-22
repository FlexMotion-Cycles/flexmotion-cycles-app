package pt.ua.tqs.flexmotion_cycles.data;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.ua.tqs.flexmotion_cycles.domain.MaintenanceTicket;
import pt.ua.tqs.flexmotion_cycles.domain.TicketStatus;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;

public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, Long> {

    boolean existsByVehicleAndStatus(Vehicle vehicle, TicketStatus status);
}
