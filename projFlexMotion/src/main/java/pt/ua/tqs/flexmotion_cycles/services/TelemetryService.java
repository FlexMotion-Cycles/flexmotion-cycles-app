package pt.ua.tqs.flexmotion_cycles.services;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.TelemetryReading;

@Service
public class TelemetryService {

    private final VehicleRepository vehicleRepository;

    public TelemetryService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @EventListener
    @Transactional
    public void onTelemetry(TelemetryReading reading) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
