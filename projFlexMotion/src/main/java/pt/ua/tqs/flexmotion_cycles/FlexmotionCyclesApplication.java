package pt.ua.tqs.flexmotion_cycles;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan 
public class FlexmotionCyclesApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlexmotionCyclesApplication.class, args);
	}

}
