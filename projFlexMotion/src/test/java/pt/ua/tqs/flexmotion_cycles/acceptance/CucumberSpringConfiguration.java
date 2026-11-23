package pt.ua.tqs.flexmotion_cycles.acceptance;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

/** Full application with PostgreSQL in a container for the acceptance scenarios. */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("it")
@AutoConfigureMockMvc
public class CucumberSpringConfiguration {
}
