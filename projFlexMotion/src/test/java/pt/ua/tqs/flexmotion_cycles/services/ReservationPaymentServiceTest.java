package pt.ua.tqs.flexmotion_cycles.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import pt.ua.tqs.flexmotion_cycles.data.PaymentRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Payment;
import pt.ua.tqs.flexmotion_cycles.domain.PaymentMethod;
import pt.ua.tqs.flexmotion_cycles.domain.PaymentStatus;
import pt.ua.tqs.flexmotion_cycles.domain.PaymentType;
import pt.ua.tqs.flexmotion_cycles.domain.Reservation;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.StationType;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;
import pt.ua.tqs.flexmotion_cycles.ports.PaymentPort;
import pt.ua.tqs.flexmotion_cycles.ports.PaymentResult;

@ExtendWith(MockitoExtension.class)
class ReservationPaymentServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-30T07:45:00Z");
    private static final Instant EXPIRED_AT = Instant.parse("2026-11-30T08:00:00Z");
    private static final String USERNAME = "ines";
    private static final BigDecimal FEE = new BigDecimal("0.50");

    @Mock
    private DepositService depositService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentPort paymentPort;

    private ReservationPaymentService service;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        service = new ReservationPaymentService(depositService, paymentRepository, paymentPort);
        Station central = new Station("Central", 40.6405, -8.6538, 10, StationType.CONVENTIONAL);
        Vehicle ebike = new Vehicle("EB-01", VehicleType.E_BIKE, 80, central);
        reservation = new Reservation(USERNAME, ebike, CREATED_AT, Duration.ofMinutes(15), FEE);
        when(depositService.requireDefaultCard(USERNAME))
                .thenReturn(new PaymentMethod(USERNAME, "pm_card_visa", "4242"));
        when(paymentRepository.save(any(Payment.class))).then(returnsFirstArg());
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @Requirement("FM-29")
    void approvedFeeIsCapturedForTheReservation() {
        when(paymentPort.charge("pm_card_visa", FEE)).thenReturn(PaymentResult.approved("ch_1"));

        Payment fee = service.chargeFee(reservation, EXPIRED_AT);

        assertThat(fee.getType()).isEqualTo(PaymentType.RESERVATION_FEE);
        assertThat(fee.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(fee.getAmount()).isEqualByComparingTo(FEE);
        assertThat(fee.getReservation()).isSameAs(reservation);
        assertThat(fee.getCreatedAt()).isEqualTo(EXPIRED_AT);
        verify(paymentRepository).save(fee);
    }

    @Test
    @Requirement("FM-29")
    void declinedFeeIsKeptAsDeclined() {
        when(paymentPort.charge("pm_card_visa", FEE)).thenReturn(PaymentResult.declined());

        Payment fee = service.chargeFee(reservation, EXPIRED_AT);

        assertThat(fee.getStatus()).isEqualTo(PaymentStatus.DECLINED);
        verify(paymentRepository).save(fee);
    }

    @Test
    @Requirement("FM-29")
    void rolledBackExpiryRefundsTheCharge() {
        TransactionSynchronizationManager.initSynchronization();
        when(paymentPort.charge("pm_card_visa", FEE)).thenReturn(PaymentResult.approved("ch_1"));
        service.chargeFee(reservation, EXPIRED_AT);

        TransactionSynchronizationManager.getSynchronizations().forEach(synchronization ->
                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(paymentPort).refund("ch_1", FEE);
    }
}
