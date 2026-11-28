package pt.ua.tqs.flexmotion_cycles.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import pt.ua.tqs.flexmotion_cycles.domain.VehicleStatus;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.ReservationRefusedException;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-30T07:45:00Z");
    private static final Instant HOLD_ENDS_AT = Instant.parse("2026-11-30T08:00:00Z");
    private static final String USERNAME = "ines";
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

    private ReservationService service;
    private Station central;

    @BeforeEach
    void setUp() {
        service = new ReservationService(vehicleRepository, reservationRepository, tripRepository,
                new ConventionalVehicleEligibility(maintenanceTicketRepository, tripRepository),
                RESERVATION, Clock.fixed(NOW, ZoneOffset.UTC));
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
    void vehicleAtMinimumBatteryCanBeReserved() {
        Vehicle ebike = vehicleFound("EB-01", VehicleType.E_BIKE, 20);
        savedReservationsAreReturned();

        service.reserve(USERNAME, "EB-01");

        assertThat(ebike.getStatus()).isEqualTo(VehicleStatus.RESERVED);
    }

    @Test
    @Requirement("FM-29")
    void refusesVehicleBelowMinimumBattery() {
        Vehicle ebike = vehicleFound("EB-04", VehicleType.E_BIKE, 19);

        assertRefused("EB-04", ebike, VehicleStatus.AVAILABLE);
    }

    @ParameterizedTest
    @EnumSource(value = VehicleStatus.class,
            names = {"RESERVED", "IN_USE", "MAINTENANCE", "REMOVED"})
    @Requirement("FM-29")
    void refusesVehicleThatIsNotAvailable(VehicleStatus status) {
        Vehicle ebike = vehicleFound("EB-05", VehicleType.E_BIKE, 80);
        ReflectionTestUtils.setField(ebike, "status", status);

        assertRefused("EB-05", ebike, status);
    }

    @Test
    @Requirement("FM-29")
    void refusesVehicleWithOpenTicket() {
        Vehicle ebike = vehicleFound("EB-03", VehicleType.E_BIKE, 80);
        when(maintenanceTicketRepository.existsByVehicleAndStatus(ebike, TicketStatus.OPEN))
                .thenReturn(true);

        assertRefused("EB-03", ebike, VehicleStatus.AVAILABLE);
    }

    @Test
    @Requirement("FM-29")
    void refusesAccessibleVehicle() {
        Vehicle wheelchairBike = vehicleFound("WB-02", VehicleType.WHEELCHAIR_BIKE, 90);

        assertRefused("WB-02", wheelchairBike, VehicleStatus.AVAILABLE);
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
    void refusesUnknownVehicle() {
        when(vehicleRepository.findLockedByCode("EB-99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reserve(USERNAME, "EB-99"))
                .isInstanceOf(VehicleNotFoundException.class);
        verify(reservationRepository, never()).save(any());
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
