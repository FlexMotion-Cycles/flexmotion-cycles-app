package pt.ua.tqs.flexmotion_cycles.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;
import pt.ua.tqs.flexmotion_cycles.data.StationRepository;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.GeoPoint;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.StationType;
import pt.ua.tqs.flexmotion_cycles.domain.TelemetryReading;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("it")
@Transactional
class TelemetryUnlockEligibilityIT {

    private static final String VEHICLE_CODE = "EB-BR4";
    private static final String USERNAME = "telemetry-it-user";
    private static final GeoPoint STATION_LOCATION = new GeoPoint(40.6405, -8.6538);
    private static final Instant READ_AT = Instant.parse("2026-11-22T16:05:00Z");
    private static final int INITIAL_BATTERY = 80;

    private final StationRepository stationRepository;
    private final VehicleRepository vehicleRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TripService tripService;

    @Autowired
    TelemetryUnlockEligibilityIT(StationRepository stationRepository,
            VehicleRepository vehicleRepository, ApplicationEventPublisher eventPublisher,
            TripService tripService) {
        this.stationRepository = stationRepository;
        this.vehicleRepository = vehicleRepository;
        this.eventPublisher = eventPublisher;
        this.tripService = tripService;
    }

    @BeforeEach
    void setUp() {
        Station station = stationRepository.save(new Station("Battery Check",
                STATION_LOCATION.latitude(), STATION_LOCATION.longitude(), 10,
                StationType.CONVENTIONAL));
        vehicleRepository.save(
                new Vehicle(VEHICLE_CODE, VehicleType.E_BIKE, INITIAL_BATTERY, station));
    }

    @Test
    @Requirement("FM-26")
    void unlockIsRefusedAfterTelemetryReportsNineteenPercent() {
        reportBattery(19);

        assertThatThrownBy(() -> tripService.requestUnlock(USERNAME, VEHICLE_CODE))
                .isInstanceOf(UnlockRefusedException.class);
    }

    @Test
    @Requirement("FM-26")
    void unlockIsAcceptedAfterTelemetryReportsTwentyPercent() {
        reportBattery(20);

        assertThat(tripService.requestUnlock(USERNAME, VEHICLE_CODE)).isNotNull();
    }

    private void reportBattery(int batteryPercent) {
        eventPublisher.publishEvent(new TelemetryReading(VEHICLE_CODE, READ_AT, batteryPercent,
                STATION_LOCATION, true, null));
    }
}
