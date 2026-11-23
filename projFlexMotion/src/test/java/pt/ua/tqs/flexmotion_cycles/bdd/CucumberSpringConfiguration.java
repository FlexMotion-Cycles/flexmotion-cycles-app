package pt.ua.tqs.flexmotion_cycles.bdd;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;

import io.cucumber.spring.CucumberContextConfiguration;

@CucumberContextConfiguration 
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(TestcontainersConfiguration.class)
public class CucumberSpringConfiguration {
    
}
