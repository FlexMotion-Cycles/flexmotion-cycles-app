package pt.ua.tqs.fleet_simulator.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import pt.ua.tqs.fleet_simulator.services.FleetSimulator;

/** Runs a telemetry round every "simulator.tick-interval". */
@Configuration
@EnableScheduling
public class TelemetryScheduleConfig implements SchedulingConfigurer {

    private final FleetSimulator fleetSimulator;
    private final SimulatorProperties properties;

    public TelemetryScheduleConfig(FleetSimulator fleetSimulator, SimulatorProperties properties) {
        this.fleetSimulator = fleetSimulator;
        this.properties = properties;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addFixedRateTask(fleetSimulator::publishRound, properties.tickInterval());
    }
}