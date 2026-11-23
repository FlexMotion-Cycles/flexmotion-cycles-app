package pt.ua.tqs.fleet_simulator.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Readings take their time from this clock, so tests can fix it (AGENTS.md §2). */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}