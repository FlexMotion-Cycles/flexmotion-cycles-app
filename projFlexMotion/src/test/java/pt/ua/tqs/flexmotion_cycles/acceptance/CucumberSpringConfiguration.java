package pt.ua.tqs.flexmotion_cycles.acceptance;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import io.cucumber.spring.CucumberContextConfiguration;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;

/** Spring context shared by all scenarios: the full application with PostgreSQL. */
@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public class CucumberSpringConfiguration {
}