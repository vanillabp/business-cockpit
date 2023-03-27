package io.vanillabp.cockpit.simulator;

import io.vanillabp.cockpit.simulator.config.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

import java.util.Map;

@SpringBootApplication
@ComponentScan(basePackageClasses = BusinessCockpitSimulator.class)
@EnableConfigurationProperties({ ApplicationProperties.class })
public class BusinessCockpitSimulator {

    public static void main(String... args) {

        final var app = new SpringApplication(BusinessCockpitSimulator.class);
        app.setDefaultProperties(
                Map.of("spring.profiles.default", "rest-sync"));
        app.run(args);
        
    }

}
