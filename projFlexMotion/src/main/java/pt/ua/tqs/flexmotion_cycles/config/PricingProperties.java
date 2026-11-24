package pt.ua.tqs.flexmotion_cycles.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("flexmotion.pricing")
public record PricingProperties(BigDecimal unlockFee, BigDecimal perMinuteRate, BigDecimal overtimeRate, int maxTripMinutes) {
    
}
