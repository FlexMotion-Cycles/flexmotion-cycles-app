package pt.ua.tqs.flexmotion_cycles.acceptance;

import io.cucumber.java.Before;
import pt.ua.tqs.flexmotion_cycles.adapters.telemetry.FakeTelemetryAdapter;
import pt.ua.tqs.flexmotion_cycles.data.MaintenanceTicketRepository;
import pt.ua.tqs.flexmotion_cycles.data.StationRepository;
import pt.ua.tqs.flexmotion_cycles.data.TripRepository;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;

/** Clears the shared database before every scenario, children before parents (foreign keys). */
public class DatabaseCleanupHook {

    private final TripRepository tripRepository;
    private final MaintenanceTicketRepository maintenanceTicketRepository;
    private final VehicleRepository vehicleRepository;
    private final StationRepository stationRepository;
    private final FakeTelemetryAdapter telemetry;

    public DatabaseCleanupHook(TripRepository tripRepository,
            MaintenanceTicketRepository maintenanceTicketRepository,
            VehicleRepository vehicleRepository, StationRepository stationRepository,
            FakeTelemetryAdapter telemetry) {
        this.tripRepository = tripRepository;
        this.maintenanceTicketRepository = maintenanceTicketRepository;
        this.vehicleRepository = vehicleRepository;
        this.stationRepository = stationRepository;
        this.telemetry = telemetry;
    }

    @Before
    public void cleanDatabase() {
        tripRepository.deleteAll();
        maintenanceTicketRepository.deleteAll();
        vehicleRepository.deleteAll();
        stationRepository.deleteAll();
        telemetry.reset();
    }
}
