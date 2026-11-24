package pt.ua.tqs.fleet_simulator.acceptance;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import io.cucumber.spring.CucumberContextConfiguration;
import pt.ua.tqs.fleet_simulator.support.MqttBroker;

/**
 * The simulator with a one-vehicle fleet, against RabbitMQ in a container. Every scenario gets a
 * new simulator, so it starts in the state the Background describes.
 */
@CucumberContextConfiguration
@SpringBootTest(properties = {
    "simulator.tick-interval=200ms",
    "simulator.battery-drain-per-tick=1",
    "simulator.mqtt.client-id=fleet-simulator-acceptance",
    "simulator.fleet[0].code=EB-0001",
    "simulator.fleet[0].battery-percent=80",
    "simulator.fleet[0].latitude=40.6405",
    "simulator.fleet[0].longitude=-8.6538"
})
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class CucumberSpringConfiguration {

    @DynamicPropertySource
    static void brokerProperties(DynamicPropertyRegistry registry) {
        MqttBroker.registerProperties(registry);
    }
}