package pt.ua.tqs.flexmotion_cycles.acceptance;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Replaces the system clock in the acceptance context, so scenarios control the time. */
@TestConfiguration(proxyBeanMethods = false)
public class AcceptanceClockConfiguration {

    @Bean
    @Primary
    MutableClock acceptanceClock() {
        return new MutableClock();
    }
}
