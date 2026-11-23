package pt.ua.tqs.flexmotion_cycles.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.TelemetryReading;

@Service
public class TelemetryService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);

    private final VehicleRepository vehicleRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TelemetryService(VehicleRepository vehicleRepository,
            ApplicationEventPublisher eventPublisher) {
        this.vehicleRepository = vehicleRepository;
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    @Transactional
    public void onTelemetry(TelemetryReading reading) {
        vehicleRepository.findByCode(reading.vehicleCode()).ifPresentOrElse(
                vehicle -> vehicle.applyTelemetry(reading),
                () -> log.warn("Discarded telemetry for unknown vehicle {}",
                        reading.vehicleCode()));
    }
}
