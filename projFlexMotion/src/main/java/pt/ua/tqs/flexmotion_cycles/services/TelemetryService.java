package pt.ua.tqs.flexmotion_cycles.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ua.tqs.flexmotion_cycles.data.StationRepository;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.TelemetryReading;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.ports.LockOpenedEvent;

@Service
public class TelemetryService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);

    private final VehicleRepository vehicleRepository;
    private final StationRepository stationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final double dockingRadiusMeters;

    public TelemetryService(VehicleRepository vehicleRepository,
            StationRepository stationRepository, ApplicationEventPublisher eventPublisher,
            @Value("${flexmotion.telemetry.docking-radius-meters}") double dockingRadiusMeters) {
        this.vehicleRepository = vehicleRepository;
        this.stationRepository = stationRepository;
        this.eventPublisher = eventPublisher;
        this.dockingRadiusMeters = dockingRadiusMeters;
    }

    @EventListener
    @Transactional
    public void onTelemetry(TelemetryReading reading) {
        vehicleRepository.findByCode(reading.vehicleCode()).ifPresentOrElse(
                vehicle -> apply(vehicle, reading),
                () -> log.warn("Discarded telemetry for unknown vehicle {}",
                        reading.vehicleCode()));
    }

    private void apply(Vehicle vehicle, TelemetryReading reading) {
        boolean wasLocked = vehicle.isLocked();
        if (vehicle.applyTelemetry(reading) && wasLocked && !reading.locked()) {
            eventPublisher.publishEvent(new LockOpenedEvent(vehicle.getCode(),
                    reading.timestamp()));
        }
    }
}
