package pt.ua.tqs.flexmotion_cycles.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Runs the @Scheduled jobs; off in the "it" profile, where tests run them explicitly. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "flexmotion.scheduling.enabled", havingValue = "true")
public class SchedulingConfig {
}
