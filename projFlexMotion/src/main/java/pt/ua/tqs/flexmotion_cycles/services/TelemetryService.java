package pt.ua.tqs.flexmotion_cycles.services;

import java.time.Clock;
import java.util.Comparator;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ua.tqs.flexmotion_cycles.data.MaintenanceTicketRepository;
import pt.ua.tqs.flexmotion_cycles.data.StationRepository;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.GeoPoint;
import pt.ua.tqs.flexmotion_cycles.domain.MaintenanceTicket;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.TelemetryReading;
import pt.ua.tqs.flexmotion_cycles.domain.TicketOrigin;
import pt.ua.tqs.flexmotion_cycles.domain.TicketStatus;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleLifecycle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleState;
import pt.ua.tqs.flexmotion_cycles.ports.LockOpenedEvent;

@Service
public class TelemetryService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);

    private final VehicleRepository vehicleRepository;
    private final StationRepository stationRepository;
    private final MaintenanceTicketRepository maintenanceTicketRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final double dockingRadiusMeters;
    private final VehicleLifecycle lifecycle = new VehicleLifecycle();

    public TelemetryService(VehicleRepository vehicleRepository,
            StationRepository stationRepository,
            MaintenanceTicketRepository maintenanceTicketRepository,
            ApplicationEventPublisher eventPublisher, Clock clock,
            @Value("${flexmotion.telemetry.docking-radius-meters}") double dockingRadiusMeters) {
        this.vehicleRepository = vehicleRepository;
        this.stationRepository = stationRepository;
        this.maintenanceTicketRepository = maintenanceTicketRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
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
        if (!vehicle.applyTelemetry(reading)) {
            return;
        }
        updateStation(vehicle, reading);
        if (wasLocked && !reading.locked()) {
            eventPublisher.publishEvent(new LockOpenedEvent(vehicle.getCode(),
                    reading.timestamp()));
        }
        if (reading.faultEvent() != null) {
            handleFault(vehicle, reading.faultEvent());
        }
    }

    private void handleFault(Vehicle vehicle, String faultCode) {
        if (vehicle.getState() == VehicleState.REMOVED) {
            return;
        }
        if (!maintenanceTicketRepository.existsByVehicleAndOriginAndStatus(
                vehicle, TicketOrigin.FAULT_EVENT, TicketStatus.OPEN)) {
            maintenanceTicketRepository.save(
                    MaintenanceTicket.forFaultEvent(vehicle, faultCode, clock.instant()));
        }
        if (vehicle.getState() == VehicleState.AVAILABLE) {
            lifecycle.apply(vehicle, VehicleState.MAINTENANCE);
        }
    }

    private void updateStation(Vehicle vehicle, TelemetryReading reading) {
        if (!reading.locked()) {
            vehicle.undock();
            return;
        }
        nearestStationWithinDockingRadius(reading.position())
                .ifPresentOrElse(vehicle::dockAt, vehicle::undock);
    }

    private Optional<Station> nearestStationWithinDockingRadius(GeoPoint position) {
        return stationRepository.findAll().stream()
                .filter(station -> station.getLocation() != null)
                .min(Comparator.comparingDouble(
                        station -> station.getLocation().distanceMetersTo(position)))
                .filter(station ->
                        station.getLocation().distanceMetersTo(position) <= dockingRadiusMeters);
    }
}
