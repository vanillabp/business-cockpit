package io.vanillabp.cockpit.simulator;

import io.vanillabp.cockpit.commons.exceptions.RestfulExceptionHandler;
import io.vanillabp.cockpit.commons.utils.LoggerFactory;
import io.vanillabp.cockpit.simulator.config.ApplicationProperties;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackageClasses = {
        BusinessCockpitSimulator.class,
        RestfulExceptionHandler.class,
        LoggerFactory.class
})
@EnableConfigurationProperties({
        ApplicationProperties.class,
        io.vanillabp.cockpit.devshell.simulator.config.Properties.class
})
public class BusinessCockpitSimulator {

    public static void main(String... args) {

        final var app = new SpringApplication(BusinessCockpitSimulator.class);
        app.setDefaultProperties(
                Map.of("spring.profiles.default", "rest-sync"));
        app.run(args);
        
    }

}
