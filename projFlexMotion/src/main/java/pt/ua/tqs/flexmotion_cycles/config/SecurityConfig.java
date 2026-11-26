package pt.ua.tqs.flexmotion_cycles.config;

import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
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

import jakarta.servlet.DispatcherType;

/**
 * Access rules of the REST API. Until JWT exists (US-01/US-02), users authenticate with
 * HTTP Basic against the spring.security.user.* account (environment variables).
 */
@Configuration
public class SecurityConfig {

    /** Web UI pages: public shells, their data comes from the (protected) REST API. */
    private static final String[] PUBLIC_PAGES = {"/", "/login", "/stations"};

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Stateless REST API authenticated per request, no session cookie to protect.
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        // Error pages (404, 500) of public requests must not turn into 401.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // Used by the Docker Compose and CI health checks.
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                                .permitAll()
                        // CSS, JS, images and favicon of the web UI.
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
                                .permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_PAGES).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/stations/**").permitAll()
                        .requestMatchers("/api/stations/**", "/api/vehicles/**")
                                .hasRole("OPERATOR")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }
}