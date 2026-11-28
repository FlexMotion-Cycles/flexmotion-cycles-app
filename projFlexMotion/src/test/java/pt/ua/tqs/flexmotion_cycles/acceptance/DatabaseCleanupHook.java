package pt.ua.tqs.flexmotion_cycles.acceptance;

import io.cucumber.java.Before;
import pt.ua.tqs.flexmotion_cycles.adapters.telemetry.FakeTelemetryAdapter;
import pt.ua.tqs.flexmotion_cycles.data.MaintenanceTicketRepository;
import pt.ua.tqs.flexmotion_cycles.data.ReservationRepository;
import pt.ua.tqs.flexmotion_cycles.data.StationRepository;
import pt.ua.tqs.flexmotion_cycles.data.TripRepository;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;

/**
 * Clears the shared database before every scenario, children before parents (foreign keys),
 * and gives the scenario the system clock back.
 */
public class DatabaseCleanupHook {

    private final TripRepository tripRepository;
    private final ReservationRepository reservationRepository;
    private final MaintenanceTicketRepository maintenanceTicketRepository;
    private final VehicleRepository vehicleRepository;
    private final StationRepository stationRepository;
    private final FakeTelemetryAdapter telemetry;
    private final MutableClock clock;

    public DatabaseCleanupHook(TripRepository tripRepository,
            ReservationRepository reservationRepository,
            MaintenanceTicketRepository maintenanceTicketRepository,
            VehicleRepository vehicleRepository, StationRepository stationRepository,
            FakeTelemetryAdapter telemetry, MutableClock clock) {
        this.tripRepository = tripRepository;
        this.reservationRepository = reservationRepository;
        this.maintenanceTicketRepository = maintenanceTicketRepository;
        this.vehicleRepository = vehicleRepository;
        this.stationRepository = stationRepository;
        this.telemetry = telemetry;
        this.clock = clock;
    }

    @Before
    public void cleanDatabase() {
        tripRepository.deleteAll();
        reservationRepository.deleteAll();
        maintenanceTicketRepository.deleteAll();
        vehicleRepository.deleteAll();
        stationRepository.deleteAll();
        telemetry.reset();
        clock.reset();
    }
}
