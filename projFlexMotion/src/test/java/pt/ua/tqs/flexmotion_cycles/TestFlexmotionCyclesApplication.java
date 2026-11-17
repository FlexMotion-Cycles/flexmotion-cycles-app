package pt.ua.tqs.flexmotion_cycles;

import org.springframework.boot.SpringApplication;

public class TestFlexmotionCyclesApplication {

	public static void main(String[] args) {
		SpringApplication.from(FlexmotionCyclesApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
