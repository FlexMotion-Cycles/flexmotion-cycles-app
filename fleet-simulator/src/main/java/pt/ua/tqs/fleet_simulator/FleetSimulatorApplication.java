package pt.ua.tqs.fleet_simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Fleet simulator: stands in for the vehicles' hardware. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class FleetSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(FleetSimulatorApplication.class, args);
    }
}