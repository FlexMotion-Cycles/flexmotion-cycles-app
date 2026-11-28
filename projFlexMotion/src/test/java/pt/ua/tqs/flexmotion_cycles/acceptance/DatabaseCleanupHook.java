package pt.ua.tqs.flexmotion_cycles.acceptance;

import io.cucumber.java.Before;
import pt.ua.tqs.flexmotion_cycles.adapters.payment.FakePaymentAdapter;
import pt.ua.tqs.flexmotion_cycles.adapters.telemetry.FakeTelemetryAdapter;
import pt.ua.tqs.flexmotion_cycles.data.MaintenanceTicketRepository;
import pt.ua.tqs.flexmotion_cycles.data.PaymentMethodRepository;
import pt.ua.tqs.flexmotion_cycles.data.PaymentRepository;
import pt.ua.tqs.flexmotion_cycles.data.StationRepository;
import pt.ua.tqs.flexmotion_cycles.data.TripRepository;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;

/** Clears the shared database before every scenario, children before parents (foreign keys). */
public class DatabaseCleanupHook {

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final TripRepository tripRepository;
    private final MaintenanceTicketRepository maintenanceTicketRepository;
    private final VehicleRepository vehicleRepository;
    private final StationRepository stationRepository;
    private final FakeTelemetryAdapter telemetry;
    private final FakePaymentAdapter payments;

    public DatabaseCleanupHook(PaymentRepository paymentRepository,
            PaymentMethodRepository paymentMethodRepository, TripRepository tripRepository,
            MaintenanceTicketRepository maintenanceTicketRepository,
            VehicleRepository vehicleRepository, StationRepository stationRepository,
            FakeTelemetryAdapter telemetry, FakePaymentAdapter payments) {
        this.paymentRepository = paymentRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.tripRepository = tripRepository;
        this.maintenanceTicketRepository = maintenanceTicketRepository;
        this.vehicleRepository = vehicleRepository;
        this.stationRepository = stationRepository;
        this.telemetry = telemetry;
        this.payments = payments;
    }

    @Before
    public void cleanDatabase() {
        paymentRepository.deleteAll();
        paymentMethodRepository.deleteAll();
        tripRepository.deleteAll();
        maintenanceTicketRepository.deleteAll();
        vehicleRepository.deleteAll();
        stationRepository.deleteAll();
        telemetry.reset();
        payments.reset();
    }
}
