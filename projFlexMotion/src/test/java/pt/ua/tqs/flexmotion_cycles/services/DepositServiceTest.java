package pt.ua.tqs.flexmotion_cycles.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.config.PaymentProperties;
import pt.ua.tqs.flexmotion_cycles.data.PaymentMethodRepository;
import pt.ua.tqs.flexmotion_cycles.data.PaymentRepository;
import pt.ua.tqs.flexmotion_cycles.domain.GeoPoint;
import pt.ua.tqs.flexmotion_cycles.domain.Payment;
import pt.ua.tqs.flexmotion_cycles.domain.PaymentMethod;
import pt.ua.tqs.flexmotion_cycles.domain.PaymentStatus;
import pt.ua.tqs.flexmotion_cycles.domain.PaymentType;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.Trip;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;
import pt.ua.tqs.flexmotion_cycles.ports.PaymentPort;
import pt.ua.tqs.flexmotion_cycles.ports.PaymentResult;

@ExtendWith(MockitoExtension.class)
class DepositServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-22T10:15:00Z");
    private static final String INES = "ines";
    private static final BigDecimal DEPOSIT = new BigDecimal("10.00");

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentPort paymentPort;

    private DepositService depositService;
    private PaymentMethod card;
    private Trip trip;

    @BeforeEach
    void setUp() {
        depositService = new DepositService(paymentMethodRepository, paymentRepository,
                paymentPort, new PaymentProperties(DEPOSIT, "eur"),
                Clock.fixed(NOW, ZoneOffset.UTC));
        card = new PaymentMethod(INES, "pm_card_visa", "4242");
        Station central = Station.conventional("Central", new GeoPoint(40.6405, -8.6538), 10, 10.0);
        trip = new Trip(INES, new Vehicle("EB-01", VehicleType.E_BIKE, central), NOW);
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @Requirement("FM-32")
    void returnsTheDefaultCardOfTheUser() {
        when(paymentMethodRepository.findByUsernameAndDefaultMethodTrue(INES))
                .thenReturn(Optional.of(card));

        assertThat(depositService.requireDefaultCard(INES)).isSameAs(card);
    }

    @Test
    @Requirement("FM-32")
    void refusesAUserWithoutALinkedCard() {
        when(paymentMethodRepository.findByUsernameAndDefaultMethodTrue(INES))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> depositService.requireDefaultCard(INES))
                .isInstanceOf(UnlockRefusedException.class);
    }

    @Test
    @Requirement("FM-32")
    void anApprovedAuthorizationIsSavedAsTheAuthorizedDepositOfTheTrip() {
        when(paymentPort.authorize("pm_card_visa", DEPOSIT))
                .thenReturn(PaymentResult.approved("pi_1"));
        when(paymentRepository.save(any(Payment.class))).then(returnsFirstArg());

        Payment deposit = depositService.authorizeDeposit(trip, card);

        assertThat(deposit.getType()).isEqualTo(PaymentType.DEPOSIT);
        assertThat(deposit.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(deposit.getAmount()).isEqualByComparingTo("10.00");
        assertThat(deposit.getGatewayRef()).isEqualTo("pi_1");
        assertThat(deposit.getPaymentMethod()).isSameAs(card);
        assertThat(deposit.getTrip()).isSameAs(trip);
        assertThat(deposit.getCreatedAt()).isEqualTo(NOW);
        verify(paymentRepository).save(deposit);
    }

    @Test
    @Requirement("FM-32")
    void aDeclinedAuthorizationRefusesTheTripAndSavesNothing() {
        when(paymentPort.authorize("pm_card_visa", DEPOSIT)).thenReturn(PaymentResult.declined());

        assertThatThrownBy(() -> depositService.authorizeDeposit(trip, card))
                .isInstanceOf(UnlockRefusedException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @Requirement("FM-32")
    void aRolledBackUnlockReleasesTheAuthorizedDeposit() {
        TransactionSynchronizationManager.initSynchronization();
        when(paymentPort.authorize("pm_card_visa", DEPOSIT))
                .thenReturn(PaymentResult.approved("pi_1"));
        when(paymentRepository.save(any(Payment.class))).then(returnsFirstArg());
        depositService.authorizeDeposit(trip, card);

        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(paymentPort).release("pi_1");
    }

    @Test
    @Requirement("FM-32")
    void aCommittedUnlockKeepsTheDepositAuthorized() {
        TransactionSynchronizationManager.initSynchronization();
        when(paymentPort.authorize("pm_card_visa", DEPOSIT))
                .thenReturn(PaymentResult.approved("pi_1"));
        when(paymentRepository.save(any(Payment.class))).then(returnsFirstArg());
        depositService.authorizeDeposit(trip, card);

        completeTransaction(TransactionSynchronization.STATUS_COMMITTED);

        verify(paymentPort, never()).release(any());
    }

    /** Simulates the end of the surrounding transaction, as Spring does after commit/rollback. */
    private static void completeTransaction(int status) {
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(status));
    }
}
