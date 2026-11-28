package pt.ua.tqs.flexmotion_cycles.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import pt.ua.tqs.flexmotion_cycles.config.ReservationProperties;
import pt.ua.tqs.flexmotion_cycles.data.MaintenanceTicketRepository;
import pt.ua.tqs.flexmotion_cycles.data.ReservationRepository;
import pt.ua.tqs.flexmotion_cycles.data.TripRepository;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Reservation;
import pt.ua.tqs.flexmotion_cycles.domain.ReservationStatus;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.StationType;
import pt.ua.tqs.flexmotion_cycles.domain.TicketStatus;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleState;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleStatus;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.PaymentMethodRequiredException;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.ReservationNotFoundException;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.ReservationRefusedException;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-30T07:45:00Z");
    private static final Instant HOLD_ENDS_AT = Instant.parse("2026-11-30T08:00:00Z");
    private static final Instant LAST_SECOND = HOLD_ENDS_AT.minusSeconds(1);
    private static final String USERNAME = "ines";
    private static final String OTHER_USER = "rui";
    private static final BigDecimal FEE = new BigDecimal("0.50");
    private static final ReservationProperties RESERVATION =
            new ReservationProperties(Duration.ofMinutes(15), FEE);

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private MaintenanceTicketRepository maintenanceTicketRepository;

    @Mock
    private ReservationPaymentService reservationPayments;

    private ReservationService service;
    private Station central;

    @BeforeEach
    void setUp() {
        service = serviceAt(NOW);
        central = new Station("Central", 40.6405, -8.6538, 10, StationType.CONVENTIONAL);
    }

    @Test
    @Requirement("FM-29")
    void reservationHoldsTheVehicleForFifteenMinutesForTheFee() {
        Vehicle ebike = vehicleFound("EB-01", VehicleType.E_BIKE, 80);
        savedReservationsAreReturned();

        Reservation reservation = service.reserve(USERNAME, "EB-01");

        assertThat(ebike.getStatus()).isEqualTo(VehicleStatus.RESERVED);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(reservation.getUsername()).isEqualTo(USERNAME);
        assertThat(reservation.getVehicle()).isSameAs(ebike);
        assertThat(reservation.getCreatedAt()).isEqualTo(NOW);
        assertThat(reservation.getExpiresAt()).isEqualTo(HOLD_ENDS_AT);
        assertThat(reservation.getFee()).isEqualByComparingTo(FEE);
        verify(reservationRepository).save(reservation);
    }

    @Test
    @Requirement("FM-29")
    void refusesVehicleBelowMinimumBattery() {
        Vehicle ebike = vehicleFound("EB-04", VehicleType.E_BIKE, 19);

        assertRefused("EB-04", ebike, VehicleStatus.AVAILABLE);
    }

    @Test
    @Requirement("FM-29")
    void refusesSecondActiveReservationOfTheUser() {
        Vehicle ebike = vehicleFound("EB-02", VehicleType.E_BIKE, 80);
        Vehicle reservedEbike = new Vehicle("EB-01", VehicleType.E_BIKE, 80, central);
        when(reservationRepository.findByUsernameAndStatus(USERNAME, ReservationStatus.ACTIVE))
                .thenReturn(List.of(new Reservation(USERNAME, reservedEbike, NOW,
                        RESERVATION.hold(), FEE)));

        assertRefused("EB-02", ebike, VehicleStatus.AVAILABLE);
    }

    @Test
    @Requirement("FM-29")
    void refusesUserWithTripInProgress() {
        Vehicle ebike = vehicleFound("EB-01", VehicleType.E_BIKE, 80);
        when(tripRepository.existsByUsernameAndStatusIn(USERNAME,
                ConventionalUseRules.TRIP_IN_PROGRESS)).thenReturn(true);

        assertRefused("EB-01", ebike, VehicleStatus.AVAILABLE);
    }

    @Test
    @Requirement("FM-29")
    void refusesUserWithoutALinkedCard() {
        Vehicle ebike = vehicleFound("EB-01", VehicleType.E_BIKE, 80);
        doThrow(new PaymentMethodRequiredException()).when(reservationPayments).requireCard(USERNAME);

        assertThatThrownBy(() -> service.reserve(USERNAME, "EB-01"))
                .isInstanceOf(PaymentMethodRequiredException.class);
        verify(reservationRepository, never()).save(any());
        assertThat(ebike.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }

    @Test
    @Requirement("FM-29")
    void refusesUnknownVehicle() {
        when(vehicleRepository.findLockedByCode("EB-99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reserve(USERNAME, "EB-99"))
                .isInstanceOf(VehicleNotFoundException.class);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @Requirement("FM-29")
    void reservationIsNotExpiredOneSecondBeforeTheHoldEnds() {
        Reservation reservation = activeReservationFound(1L);

        boolean expired = serviceAt(LAST_SECOND).expireIfDue(1L);

        assertThat(expired).isFalse();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(reservation.getVehicle().getStatus()).isEqualTo(VehicleStatus.RESERVED);
    }

    @Test
    @Requirement("FM-29")
    void reservationExpiresWhenTheHoldEndsAndTheVehicleIsAvailableAgain() {
        Reservation reservation = activeReservationFound(1L);

        boolean expired = serviceAt(HOLD_ENDS_AT).expireIfDue(1L);

        assertThat(expired).isTrue();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(reservation.getVehicle().getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }

    @Test
    @Requirement("FM-29")
    void expiredReservationIsChargedItsFee() {
        Reservation reservation = activeReservationFound(1L);

        serviceAt(HOLD_ENDS_AT).expireIfDue(1L);

        verify(reservationPayments).chargeFee(reservation, HOLD_ENDS_AT);
    }

    @Test
    @Requirement("FM-29")
    void reservationThatIsNoLongerActiveIsNotExpired() {
        Reservation reservation = activeReservationFound(1L);
        reservation.fulfil(LAST_SECOND);

        boolean expired = serviceAt(HOLD_ENDS_AT).expireIfDue(1L);

        assertThat(expired).isFalse();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.FULFILLED);
    }

    @Test
    @Requirement("FM-29")
    void dueReservationIdsAreThoseWhoseHoldHasEnded() {
        Reservation due = reservedBy(OTHER_USER, reservedVehicle("EB-01"));
        ReflectionTestUtils.setField(due, "id", 7L);
        when(reservationRepository.findByStatusAndExpiresAtLessThanEqual(
                ReservationStatus.ACTIVE, HOLD_ENDS_AT)).thenReturn(List.of(due));

        assertThat(serviceAt(HOLD_ENDS_AT).findDueReservationIds()).containsExactly(7L);
    }

    @Test
    @Requirement("FM-29")
    void ownDueReservationDoesNotBlockANewOne() {
        Reservation previous = reservedBy(USERNAME, reservedVehicle("EB-01"));
        when(reservationRepository.findByUsernameAndStatus(USERNAME, ReservationStatus.ACTIVE))
                .thenReturn(List.of(previous));
        Vehicle ebike = vehicleFound("EB-02", VehicleType.E_BIKE, 80);
        savedReservationsAreReturned();

        Reservation reservation = serviceAt(HOLD_ENDS_AT).reserve(USERNAME, "EB-02");

        assertThat(previous.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(previous.getVehicle().getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(reservation.getVehicle()).isSameAs(ebike);
        assertThat(ebike.getStatus()).isEqualTo(VehicleStatus.RESERVED);
    }

    @Test
    @Requirement("FM-29")
    void vehicleWithDueReservationOfAnotherUserCanBeReserved() {
        Vehicle ebike = vehicleFound("EB-01", VehicleType.E_BIKE, 80);
        ebike.setState(VehicleState.RESERVED);
        Reservation othersReservation = reservedBy(OTHER_USER, ebike);
        when(reservationRepository.findByVehicleAndStatus(ebike, ReservationStatus.ACTIVE))
                .thenReturn(Optional.of(othersReservation));
        savedReservationsAreReturned();

        Reservation reservation = serviceAt(HOLD_ENDS_AT).reserve(USERNAME, "EB-01");

        assertThat(othersReservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(reservation.getUsername()).isEqualTo(USERNAME);
        assertThat(ebike.getStatus()).isEqualTo(VehicleStatus.RESERVED);
    }

    @Test
    @Requirement("FM-29")
    void vehicleReservedByAnotherUserIsRefusedBeforeTheHoldEnds() {
        Vehicle ebike = vehicleFound("EB-01", VehicleType.E_BIKE, 80);
        ebike.setState(VehicleState.RESERVED);
        Reservation othersReservation = reservedBy(OTHER_USER, ebike);
        when(reservationRepository.findByVehicleAndStatus(ebike, ReservationStatus.ACTIVE))
                .thenReturn(Optional.of(othersReservation));
        service = serviceAt(LAST_SECOND);

        assertRefused("EB-01", ebike, VehicleStatus.RESERVED);
        assertThat(othersReservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @Requirement("FM-29")
    void ownerUnlockOneSecondBeforeTheHoldEndsFulfilsTheReservation() {
        Vehicle ebike = reservedVehicle("EB-01");
        Reservation reservation = holdingReservation(USERNAME, ebike);

        Optional<Reservation> fulfilled = service.fulfilForUnlock(USERNAME, ebike, LAST_SECOND);

        assertThat(fulfilled).containsSame(reservation);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.FULFILLED);
        verify(reservationPayments, never()).chargeFee(any(), any());
    }

    @Test
    @Requirement("FM-29")
    void ownerUnlockWhenTheHoldEndsExpiresTheReservation() {
        Vehicle ebike = reservedVehicle("EB-01");
        Reservation reservation = holdingReservation(USERNAME, ebike);

        Optional<Reservation> fulfilled = service.fulfilForUnlock(USERNAME, ebike, HOLD_ENDS_AT);

        assertThat(fulfilled).isEmpty();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(ebike.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }

    @Test
    @Requirement("FM-29")
    void unlockByAnotherUserBeforeTheHoldEndsIsRefused() {
        Vehicle ebike = reservedVehicle("EB-01");
        Reservation reservation = holdingReservation(OTHER_USER, ebike);

        assertThatThrownBy(() -> service.fulfilForUnlock(USERNAME, ebike, LAST_SECOND))
                .isInstanceOf(UnlockRefusedException.class);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(ebike.getStatus()).isEqualTo(VehicleStatus.RESERVED);
    }

    @Test
    @Requirement("FM-29")
    void unlockByAnotherUserWhenTheHoldEndsIsNotBlocked() {
        Vehicle ebike = reservedVehicle("EB-01");
        Reservation reservation = holdingReservation(OTHER_USER, ebike);

        Optional<Reservation> fulfilled = service.fulfilForUnlock(USERNAME, ebike, HOLD_ENDS_AT);

        assertThat(fulfilled).isEmpty();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(ebike.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }

    @Test
    @Requirement("FM-29")
    void ownerUnlockOfReservedVehicleWithOpenTicketIsRefused() {
        Vehicle ebike = reservedVehicle("EB-01");
        Reservation reservation = holdingReservation(USERNAME, ebike);
        when(maintenanceTicketRepository.existsByVehicleAndStatus(ebike, TicketStatus.OPEN))
                .thenReturn(true);

        assertThatThrownBy(() -> service.fulfilForUnlock(USERNAME, ebike, LAST_SECOND))
                .isInstanceOf(UnlockRefusedException.class);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @Requirement("FM-29")
    void reservationOfAnotherUserOrUnknownIsNotFound() {
        when(reservationRepository.findByIdAndUsername(7L, USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findReservation(USERNAME, 7L))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    /** The given user's ACTIVE reservation, found as the one holding the vehicle. */
    private Reservation holdingReservation(String username, Vehicle vehicle) {
        Reservation reservation = reservedBy(username, vehicle);
        when(reservationRepository.findByVehicleAndStatus(vehicle, ReservationStatus.ACTIVE))
                .thenReturn(Optional.of(reservation));
        return reservation;
    }

    private ReservationService serviceAt(Instant now) {
        return new ReservationService(vehicleRepository, reservationRepository, tripRepository,
                new ConventionalVehicleEligibility(maintenanceTicketRepository, tripRepository),
                reservationPayments, RESERVATION, Clock.fixed(now, ZoneOffset.UTC));
    }

    /** A vehicle already RESERVED, as it is while its reservation is ACTIVE. */
    private Vehicle reservedVehicle(String code) {
        Vehicle vehicle = new Vehicle(code, VehicleType.E_BIKE, 80, central);
        vehicle.setState(VehicleState.RESERVED);
        return vehicle;
    }

    /** An ACTIVE reservation made at NOW, so its hold ends at HOLD_ENDS_AT. */
    private Reservation reservedBy(String username, Vehicle vehicle) {
        return new Reservation(username, vehicle, NOW, RESERVATION.hold(), FEE);
    }

    /** Inês's ACTIVE reservation of EB-01, found by the given id. */
    private Reservation activeReservationFound(Long id) {
        Reservation reservation = reservedBy(USERNAME, reservedVehicle("EB-01"));
        when(reservationRepository.findById(id)).thenReturn(Optional.of(reservation));
        return reservation;
    }

    private Vehicle vehicleFound(String code, VehicleType type, int batteryLevel) {
        Vehicle vehicle = new Vehicle(code, type, batteryLevel, central);
        when(vehicleRepository.findLockedByCode(code)).thenReturn(Optional.of(vehicle));
        return vehicle;
    }

    private void savedReservationsAreReturned() {
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void assertRefused(String vehicleCode, Vehicle vehicle, VehicleStatus unchanged) {
        assertThatThrownBy(() -> service.reserve(USERNAME, vehicleCode))
                .isInstanceOf(ReservationRefusedException.class);
        verify(reservationRepository, never()).save(any());
        assertThat(vehicle.getStatus()).isEqualTo(unchanged);
    }
}
