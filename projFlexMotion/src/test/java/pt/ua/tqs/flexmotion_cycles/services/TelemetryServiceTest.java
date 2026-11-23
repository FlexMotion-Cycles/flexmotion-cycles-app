package pt.ua.tqs.flexmotion_cycles.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.GeoPoint;
import pt.ua.tqs.flexmotion_cycles.domain.TelemetryReading;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    private static final String CODE = "EB-0001";
    private static final GeoPoint POSITION = new GeoPoint(40.6405, -8.6538);

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private TelemetryService telemetryService;

    @Test
    @Requirement("FM-26")
    void updatesKnownVehicleFromReading() {
        Vehicle vehicle = new Vehicle(CODE, VehicleType.E_BIKE, null);
        when(vehicleRepository.findByCode(CODE)).thenReturn(Optional.of(vehicle));

        telemetryService.onTelemetry(new TelemetryReading(CODE,
                Instant.parse("2026-11-22T16:05:00Z"), 19, POSITION, false, null));

        assertThat(vehicle.getBatteryLevel()).isEqualTo(19);
        assertThat(vehicle.getPosition()).isEqualTo(POSITION);
        assertThat(vehicle.isLocked()).isFalse();
    }

    @Test
    @Requirement("FM-26")
    void discardsReadingForUnknownVehicle() {
        when(vehicleRepository.findByCode("XX-9999")).thenReturn(Optional.empty());

        assertThatCode(() -> telemetryService.onTelemetry(new TelemetryReading("XX-9999",
                Instant.parse("2026-11-22T16:05:00Z"), 50, POSITION, true, null)))
                .doesNotThrowAnyException();
    }
}
