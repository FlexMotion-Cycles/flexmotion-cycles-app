package pt.ua.tqs.fleet_simulator.services;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import pt.ua.tqs.fleet_simulator.config.SimulatorProperties;
import pt.ua.tqs.fleet_simulator.config.SimulatorProperties.VehicleSeed;
import pt.ua.tqs.fleet_simulator.domain.FaultEvent;
import pt.ua.tqs.fleet_simulator.domain.GeoPoint;
import pt.ua.tqs.fleet_simulator.domain.SimulatedVehicle;
import pt.ua.tqs.fleet_simulator.ports.TelemetryPublisher;

/**
 * Simulates the whole fleet. Every tick, vehicles in use drain and every vehicle reports;
 * an unlock, a lock or a fault is reported at once. Messages for unknown vehicles are ignored.
 */
@Service
public class FleetSimulator {

    private static final Logger LOG = LoggerFactory.getLogger(FleetSimulator.class);

    private final Map<String, SimulatedVehicle> vehicles = new LinkedHashMap<>();
    private final int batteryDrainPerTick;
    private final TelemetryPublisher publisher;
    private final Clock clock;

    public FleetSimulator(SimulatorProperties properties, TelemetryPublisher publisher,
            Clock clock) {
        if (properties.batteryDrainPerTick() < 0) {
            throw new IllegalArgumentException("The battery drain per tick cannot be negative");
        }
        for (VehicleSeed seed : properties.fleet()) {
            SimulatedVehicle vehicle = new SimulatedVehicle(seed.code(), seed.batteryPercent(),
                    new GeoPoint(seed.latitude(), seed.longitude()));
            if (vehicles.putIfAbsent(vehicle.code(), vehicle) != null) {
                throw new IllegalArgumentException(
                        "Vehicle " + vehicle.code() + " appears twice in the fleet");
            }
        }
        this.batteryDrainPerTick = properties.batteryDrainPerTick();
        this.publisher = publisher;
        this.clock = clock;
    }

    /** One tick: vehicles in use lose battery, then every vehicle reports. */
    public synchronized void publishRound() {
        for (SimulatedVehicle vehicle : vehicles.values()) {
            vehicle.drain(batteryDrainPerTick);
            report(vehicle);
        }
    }

    /** Command from the platform. Unlocking an open lock reports the current state again. */
    public synchronized void unlock(String vehicleCode) {
        find(vehicleCode).ifPresent(vehicle -> {
            vehicle.unlock();
            report(vehicle);
        });
    }

    /** Simulates a user closing the lock where the vehicle is. */
    public synchronized void lock(String vehicleCode) {
        find(vehicleCode).ifPresent(vehicle -> {
            vehicle.lock();
            report(vehicle);
        });
    }

    /** Simulates a user docking the vehicle at the given position. */
    public synchronized void lockAt(String vehicleCode, GeoPoint dockPosition) {
        find(vehicleCode).ifPresent(vehicle -> {
            vehicle.lockAt(dockPosition);
            report(vehicle);
        });
    }

    /** Lets tests and demos make a vehicle report a fault. */
    public synchronized void triggerFault(String vehicleCode, FaultEvent fault) {
        find(vehicleCode).ifPresent(vehicle -> {
            vehicle.triggerFault(fault);
            report(vehicle);
        });
    }

    private Optional<SimulatedVehicle> find(String vehicleCode) {
        SimulatedVehicle vehicle = vehicles.get(vehicleCode);
        if (vehicle == null) {
            LOG.warn("Ignoring a message for unknown vehicle {}", vehicleCode);
        }
        return Optional.ofNullable(vehicle);
    }

    private void report(SimulatedVehicle vehicle) {
        publisher.publish(vehicle.takeReading(clock.instant()));
    }
}