package pt.ua.tqs.flexmotion_cycles.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Positive;

@Validated 
@ConfigurationProperties("flexmotion.trip")
public record TripProperties(@Positive double dockRadiusMeters) {
    
}
