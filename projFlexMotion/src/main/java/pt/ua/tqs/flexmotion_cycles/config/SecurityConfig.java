package pt.ua.tqs.flexmotion_cycles.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import pt.ua.tqs.flexmotion_cycles.domain.Role;

/**
 * Access rules of the REST API: each area belongs to one role and any other role gets 403
 * (US-02). Until US-01 brings stored accounts and JWT, users authenticate with HTTP Basic
 * against the spring.security.user.* account (environment variables).
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Stateless REST API authenticated per request, no session cookie to protect.
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        // Used by the Docker Compose and CI health checks.
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                                .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/stations/**").hasRole(Role.END_USER.name())
                        .requestMatchers("/api/stations/**", "/api/vehicles/**")
                                .hasRole(Role.OPERATOR.name())
                        .requestMatchers("/api/trips/**", "/api/payment-methods/**")
                                .hasRole(Role.END_USER.name())
                        .requestMatchers("/api/admin/**")
                                .hasRole(Role.ADMIN.name())
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }
}
