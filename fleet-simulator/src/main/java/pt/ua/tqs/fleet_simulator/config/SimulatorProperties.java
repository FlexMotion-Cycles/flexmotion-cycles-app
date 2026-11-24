package pt.ua.tqs.fleet_simulator.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Simulation settings from application.yml ("simulator.*").
 *
 * @param tickInterval how often every vehicle reports its telemetry
 * @param batteryDrainPerTick battery percentage a vehicle in use loses at each tick
 * @param fleet the vehicles to simulate and their initial state
 */
@ConfigurationProperties(prefix = "simulator")
public record SimulatorProperties(
        Duration tickInterval,
        int batteryDrainPerTick,
        List<VehicleSeed> fleet) {

    /** Initial state of one simulated vehicle; it starts locked. */
    public record VehicleSeed(String code, int batteryPercent, double latitude, double longitude) {
    }
}