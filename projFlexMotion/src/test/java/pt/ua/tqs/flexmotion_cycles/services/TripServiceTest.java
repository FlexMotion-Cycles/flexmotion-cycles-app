package pt.ua.tqs.flexmotion_cycles.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import pt.ua.tqs.flexmotion_cycles.config.PricingProperties;
import pt.ua.tqs.flexmotion_cycles.config.TripProperties;
import pt.ua.tqs.flexmotion_cycles.data.MaintenanceTicketRepository;
import pt.ua.tqs.flexmotion_cycles.data.StationRepository;
import pt.ua.tqs.flexmotion_cycles.data.TripRepository;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.GeoPoint;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.StationType;
import pt.ua.tqs.flexmotion_cycles.domain.TicketStatus;
import pt.ua.tqs.flexmotion_cycles.domain.Trip;
import pt.ua.tqs.flexmotion_cycles.domain.TripStatus;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleStatus;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;
import pt.ua.tqs.flexmotion_cycles.ports.LockClosedEvent;
import pt.ua.tqs.flexmotion_cycles.ports.LockOpenedEvent;
import pt.ua.tqs.flexmotion_cycles.ports.TelemetryPort;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.TripNotFoundException;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-22T10:15:00Z");
    private static final String USERNAME = "ines";
    private static final PricingProperties PRICING = new PricingProperties(new BigDecimal("1.00"), new BigDecimal("0.15"), new BigDecimal("0.30"), 90);
    private static final double DOCK_RADIUS_METERS = 5.0;
    private static final Instant STARTED_AT = Instant.parse("2026-11-22T10:00:00Z");
    private static final Instant CLOSED_AT = Instant.parse("2026-11-22T10:01:01Z");
    private static final GeoPoint CENTRAL_DOCK = new GeoPoint(40.6405, -8.6538);
    private static final GeoPoint NEAR_CENTRAL = new GeoPoint(40.64054, -8.6538);
    private static final GeoPoint OUTSIDE_CENTRAL = new GeoPoint(40.64055, -8.6538);

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private MaintenanceTicketRepository maintenanceTicketRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TelemetryPort telemetryPort;

    @Mock 
    private StationRepository stationRepository;

    private TripService service;
    private Station central;

    @BeforeEach
    void setUp() {
        service = serviceWithDockRadius(DOCK_RADIUS_METERS);
        central = new Station("Central", 40.6405, -8.6538, 10, StationType.CONVENTIONAL);
    }

    @Test
    @Requirement("FM-27")
    void unlockAtMinimumBatteryRequestsTheUnlock() {
        vehicleFound("EB-01", VehicleType.E_BIKE, 20);
        savedTripsAreReturned();

        Trip trip = service.requestUnlock(USERNAME, "EB-01");

        assertThat(trip.getStatus()).isEqualTo(TripStatus.UNLOCK_REQUESTED);
        assertThat(trip.getUsername()).isEqualTo(USERNAME);
        assertThat(trip.getRequestedAt()).isEqualTo(NOW);
        assertThat(trip.getStartedAt()).isNull();
        verify(telemetryPort).sendUnlockCommand("EB-01");
    }

    @Test
    @Requirement("FM-27")
    void vehicleStaysAvailableUntilTheLockOpens() {
        Vehicle ebike = vehicleFound("EB-01", VehicleType.E_BIKE, 80);
        savedTripsAreReturned();

        service.requestUnlock(USERNAME, "EB-01");

        assertThat(ebike.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }

    @Test
    @Requirement("FM-27")
    void refusesVehicleBelowMinimumBattery() {
        vehicleFound("EB-04", VehicleType.E_BIKE, 19);

        assertRefused("EB-04");
    }

    @ParameterizedTest
    @EnumSource(value = VehicleStatus.class,
            names = {"RESERVED", "IN_USE", "MAINTENANCE", "REMOVED"})
    @Requirement("FM-27")
    void refusesVehicleThatIsNotAvailable(VehicleStatus status) {
        Vehicle ebike = vehicleFound("EB-05", VehicleType.E_BIKE, 80);
        ReflectionTestUtils.setField(ebike, "status", status);

        assertRefused("EB-05");
    }

    @Test
    @Requirement("FM-27")
    void refusesVehicleWithOpenTicket() {
        Vehicle ebike = vehicleFound("EB-03", VehicleType.E_BIKE, 80);
        when(maintenanceTicketRepository.existsByVehicleAndStatus(ebike, TicketStatus.OPEN))
                .thenReturn(true);

        assertRefused("EB-03");
    }

    @Test
    @Requirement("FM-27")
    void refusesAccessibleVehicle() {
        vehicleFound("WB-02", VehicleType.WHEELCHAIR_BIKE, 90);

        assertRefused("WB-02");
    }

    @Test
    @Requirement("FM-27")
    void refusesVehicleWithPendingUnlock() {
        Vehicle ebike = vehicleFound("EB-01", VehicleType.E_BIKE, 80);
        when(tripRepository.existsByVehicleAndStatus(ebike, TripStatus.UNLOCK_REQUESTED))
                .thenReturn(true);

        assertRefused("EB-01");
    }

    @Test
    @Requirement("FM-27")
    void refusesUserWithTripInProgress() {
        vehicleFound("EB-01", VehicleType.E_BIKE, 80);
        when(tripRepository.existsByUsernameAndStatusIn(eq(USERNAME),
                eq(Set.of(TripStatus.UNLOCK_REQUESTED, TripStatus.ACTIVE))))
                .thenReturn(true);

        assertRefused("EB-01");
    }

    @Test
    @Requirement("FM-27")
    void refusesUnknownVehicle() {
        when(vehicleRepository.findLockedByCode("XX-99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requestUnlock(USERNAME, "XX-99"))
                .isInstanceOf(VehicleNotFoundException.class);
        verify(telemetryPort, never()).sendUnlockCommand(any());
    }

    @Test
    @Requirement("FM-27")
    void lockOpeningStartsTheTripAtTheTelemetryTime() {
        Vehicle ebike = new Vehicle("EB-01", VehicleType.E_BIKE, 80, central);
        Trip trip = new Trip(USERNAME, ebike, NOW);
        Instant openedAt = Instant.parse("2026-11-22T10:15:30Z");
        when(tripRepository.findByVehicleCodeAndStatus("EB-01", TripStatus.UNLOCK_REQUESTED))
                .thenReturn(Optional.of(trip));

        service.onLockOpened(new LockOpenedEvent("EB-01", openedAt));

        assertThat(trip.getStatus()).isEqualTo(TripStatus.ACTIVE);
        assertThat(trip.getStartedAt()).isEqualTo(openedAt);
        assertThat(trip.getStartStation()).isEqualTo(central);
        assertThat(ebike.getStatus()).isEqualTo(VehicleStatus.IN_USE);
        assertThat(ebike.getStation()).isNull();
    }

    @Test
    @Requirement("FM-27")
    void lockOpeningWithoutPendingUnlockIsIgnored() {
        when(tripRepository.findByVehicleCodeAndStatus("EB-01", TripStatus.UNLOCK_REQUESTED))
                .thenReturn(Optional.empty());

        assertThatCode(() -> service.onLockOpened(new LockOpenedEvent("EB-01", NOW)))
                .doesNotThrowAnyException();
    }

    @Test
    @Requirement("FM-28")
    void lockClosingAtADockEndsTheTripWithItsCostAndFreesTheVehicle() {
        Vehicle ebike = new Vehicle("EB-01", VehicleType.E_BIKE, 80, central);
        Trip trip = activeTripOn(ebike);
        conventionalStations(central);

        service.onLockClosed(new LockClosedEvent("EB-01", CLOSED_AT, CENTRAL_DOCK));

        assertThat(trip.getStatus()).isEqualTo(TripStatus.ENDED);
        assertThat(trip.getEndedAt()).isEqualTo(CLOSED_AT);
        assertThat(trip.getEndStation()).isSameAs(central);
        assertThat(trip.getCost().billedMinutes()).isEqualTo(2);
        assertThat(trip.getCost().total()).isEqualByComparingTo("1.30");
        assertThat(ebike.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(ebike.getStation()).isSameAs(central);
    }

    @Test
    @Requirement("FM-28")
    void lockClosingWithinTheDockRadiusEndsTheTrip() {
        Trip trip = activeTripOn(new Vehicle("EB-01", VehicleType.E_BIKE, 80, central));
        conventionalStations(central);

        service.onLockClosed(new LockClosedEvent("EB-01", CLOSED_AT, NEAR_CENTRAL));

        assertThat(trip.getStatus()).isEqualTo(TripStatus.ENDED);
    }

    @Test
    @Requirement("FM-28")
    void lockClosingExactlyAtTheDockRadiusEndsTheTrip() {
        service = serviceWithDockRadius(CENTRAL_DOCK.distanceMetersTo(NEAR_CENTRAL));
        Trip trip = activeTripOn(new Vehicle("EB-01", VehicleType.E_BIKE, 80, central));
        conventionalStations(central);

        service.onLockClosed(new LockClosedEvent("EB-01", CLOSED_AT, NEAR_CENTRAL));

        assertThat(trip.getStatus()).isEqualTo(TripStatus.ENDED);
    }

    @Test
    @Requirement("FM-28")
    void lockClosingOutsideADockKeepsTheTripRunning() {
        Vehicle ebike = new Vehicle("EB-01", VehicleType.E_BIKE, 80, central);
        Trip trip = activeTripOn(ebike);
        conventionalStations(central);

        service.onLockClosed(new LockClosedEvent("EB-01", CLOSED_AT, OUTSIDE_CENTRAL));

        assertThat(trip.getStatus()).isEqualTo(TripStatus.ACTIVE);
        assertThat(trip.getCost()).isNull();
        assertThat(ebike.getStatus()).isEqualTo(VehicleStatus.IN_USE);
        assertThat(ebike.getStation()).isNull();
    }

    @Test
    @Requirement("FM-28")
    void tripEndsAtTheClosestDockWhenSeveralAreInRange() {
        service = serviceWithDockRadius(100.0);
        Station nearby = new Station("Nearby", 40.6406, -8.6538, 10, StationType.CONVENTIONAL);
        Trip trip = activeTripOn(new Vehicle("EB-01", VehicleType.E_BIKE, 80, central));
        conventionalStations(nearby, central);

        service.onLockClosed(new LockClosedEvent("EB-01", CLOSED_AT, NEAR_CENTRAL));

        assertThat(trip.getEndStation()).isSameAs(central);
    }

    @Test
    @Requirement("FM-28")
    void lockClosingWithoutAnActiveTripIsIgnored() {
        when(tripRepository.findByVehicleCodeAndStatus("EB-01", TripStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatCode(() -> service.onLockClosed(
                new LockClosedEvent("EB-01", CLOSED_AT, CENTRAL_DOCK)))
                .doesNotThrowAnyException();
        verify(stationRepository, never()).findByType(any());
    }

    @Test
    @Requirement("FM-28")
    void findTripReturnsTheUsersOwnTrip() {
        Trip trip = new Trip(USERNAME, new Vehicle("EB-01", VehicleType.E_BIKE, 80, central), NOW);
        when(tripRepository.findByIdAndUsername(7L, USERNAME)).thenReturn(Optional.of(trip));

        assertThat(service.findTrip(USERNAME, 7L)).isSameAs(trip);
    }

    @Test
    @Requirement("FM-28")
    void tripOfAnotherUserOrUnknownIsNotFound() {
        when(tripRepository.findByIdAndUsername(7L, USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findTrip(USERNAME, 7L)).isInstanceOf(TripNotFoundException.class);
    }

    private Vehicle vehicleFound(String code, VehicleType type, int batteryLevel) {
        Vehicle vehicle = new Vehicle(code, type, batteryLevel, central);
        when(vehicleRepository.findLockedByCode(code)).thenReturn(Optional.of(vehicle));
        return vehicle;
    }

    private void savedTripsAreReturned() {
        when(tripRepository.save(any(Trip.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void assertRefused(String vehicleCode) {
        assertThatThrownBy(() -> service.requestUnlock(USERNAME, vehicleCode))
                .isInstanceOf(UnlockRefusedException.class);
        verify(tripRepository, never()).save(any());
        verify(telemetryPort, never()).sendUnlockCommand(any());
    }

    private TripService serviceWithDockRadius(double dockRadiusMeters) {
        return new TripService(vehicleRepository, new ConventionalVehicleEligibility(maintenanceTicketRepository, tripRepository), tripRepository, stationRepository, telemetryPort, new TripCostCalculator(PRICING), new TripProperties(dockRadiusMeters), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /** A trip whose lock opened at STARTED_AT, returned as the vehicle's active trip. */
    private Trip activeTripOn(Vehicle ebike) {
        Trip trip = new Trip(USERNAME, ebike, STARTED_AT);
        ebike.startUse();
        trip.start(STARTED_AT);
        when(tripRepository.findByVehicleCodeAndStatus(ebike.getCode(), TripStatus.ACTIVE)).thenReturn(Optional.of(trip));
        return trip;
    }

    private void conventionalStations(Station... stations) {
        when(stationRepository.findByType(StationType.CONVENTIONAL)).thenReturn(List.of(stations));
    }
}
