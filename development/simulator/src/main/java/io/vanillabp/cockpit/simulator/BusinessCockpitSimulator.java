package io.vanillabp.cockpit.simulator;

import io.vanillabp.cockpit.commons.exceptions.RestfulExceptionHandler;
import io.vanillabp.cockpit.commons.utils.LoggerFactory;
import io.vanillabp.cockpit.simulator.config.ApplicationProperties;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * The Spring Boot application of the simulator. It stands in for a workflow module during
 * development: it sends test user tasks and workflows to the Business Cockpit and serves the
 * official task list and workflow list API.
 */
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

    /**
     * Creates the application. Spring Boot calls it, nobody else does.
     */
    public BusinessCockpitSimulator() {
    }

    /**
     * Starts the simulator. If no Spring profile is given, the profile {@code rest-sync} is used,
     * so the simulator reports to the Business Cockpit by REST.
     *
     * @param args The command line arguments, passed on to Spring Boot
     */
    public static void main(String... args) {

        final var app = new SpringApplication(BusinessCockpitSimulator.class);
        app.setDefaultProperties(
                Map.of("spring.profiles.default", "rest-sync"));
        app.run(args);
        
    }

}
