package pt.ua.tqs.flexmotion_cycles.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;
import pt.ua.tqs.flexmotion_cycles.adapters.payment.FakePaymentAdapter;
import pt.ua.tqs.flexmotion_cycles.adapters.telemetry.FakeTelemetryAdapter;
import pt.ua.tqs.flexmotion_cycles.data.PaymentMethodRepository;
import pt.ua.tqs.flexmotion_cycles.data.PaymentRepository;
import pt.ua.tqs.flexmotion_cycles.data.StationRepository;
import pt.ua.tqs.flexmotion_cycles.data.TripRepository;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Payment;
import pt.ua.tqs.flexmotion_cycles.domain.PaymentMethod;
import pt.ua.tqs.flexmotion_cycles.domain.PaymentStatus;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.StationType;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

/** BR16 with real transactions: a rolled-back unlock must not leave the deposit reserved. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("it")
class DepositRollbackIT {

    private static final String USERNAME = "deposit-it-user";
    private static final String VEHICLE_CODE = "EB-DEP";

    private final TripService tripService;
    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;
    private final StationRepository stationRepository;
    private final FakePaymentAdapter payments;
    private final FakeTelemetryAdapter telemetry;
    private final TransactionTemplate transaction;

    @Autowired
    DepositRollbackIT(TripService tripService, PaymentRepository paymentRepository,
            PaymentMethodRepository paymentMethodRepository, TripRepository tripRepository,
            VehicleRepository vehicleRepository, StationRepository stationRepository,
            FakePaymentAdapter payments, FakeTelemetryAdapter telemetry,
            PlatformTransactionManager transactionManager) {
        this.tripService = tripService;
        this.paymentRepository = paymentRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.tripRepository = tripRepository;
        this.vehicleRepository = vehicleRepository;
        this.stationRepository = stationRepository;
        this.payments = payments;
        this.telemetry = telemetry;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @BeforeEach
    void setUp() {
        cleanUp();
        Station station = stationRepository.save(
                new Station("Deposit Central", 40.6405, -8.6538, 10, StationType.CONVENTIONAL));
        vehicleRepository.save(new Vehicle(VEHICLE_CODE, VehicleType.E_BIKE, 80, station));
        PaymentMethod card =
                new PaymentMethod(USERNAME, FakePaymentAdapter.APPROVED_CARD, "4242");
        card.makeDefault();
        paymentMethodRepository.save(card);
    }

    /** These tests commit, so they must not leave data for the other tests on this database. */
    @AfterEach
    void cleanUp() {
        paymentRepository.deleteAll();
        paymentMethodRepository.deleteAll();
        tripRepository.deleteAll();
        vehicleRepository.deleteAll();
        stationRepository.deleteAll();
        payments.reset();
        telemetry.reset();
    }

    @Test
    @Requirement("FM-32")
    void aRolledBackUnlockLeavesNoDepositReserved() {
        transaction.executeWithoutResult(status -> {
            tripService.requestUnlock(USERNAME, VEHICLE_CODE);
            assertThat(payments.openAuthorizations()).hasSize(1);
            // Stands for anything failing before the commit (e.g. a concurrent unlock).
            status.setRollbackOnly();
        });

        assertThat(payments.openAuthorizations()).isEmpty();
        assertThat(paymentRepository.count()).isZero();
        assertThat(tripRepository.count()).isZero();
    }

    @Test
    @Requirement("FM-32")
    void aCommittedUnlockKeepsTheDepositAuthorized() {
        tripService.requestUnlock(USERNAME, VEHICLE_CODE);

        assertThat(payments.openAuthorizations()).hasSize(1);
        assertThat(paymentRepository.findAll())
                .singleElement()
                .satisfies(deposit -> {
                    assertThat(deposit.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
                    assertThat(deposit.getAmount()).isEqualByComparingTo("10.00");
                    assertThat(deposit.getGatewayRef())
                            .isEqualTo(payments.openAuthorizations().get(0));
                });
    }
}
