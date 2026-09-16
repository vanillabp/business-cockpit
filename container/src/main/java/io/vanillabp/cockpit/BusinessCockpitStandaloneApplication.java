package io.vanillabp.cockpit;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The runnable business cockpit, and the shortest example of an application built on the library.
 * <p>
 * There is no base class and no cockpit annotation here. The dependency on
 * {@code business-cockpit} brings the cockpit along, through the auto-configurations of
 * {@code io.vanillabp.cockpit.autoconfigure}. A custom cockpit writes exactly this much.
 * <p>
 * This module adds more than starting Spring Boot, and a custom cockpit writes that part for
 * itself as well. It is the concrete GUI API controllers, the demo users of the profile
 * {@code local} and the {@code application*.yaml} defaults below {@code config/}. The component
 * scan of {@code @SpringBootApplication} finds them, because it reaches the packages below this
 * one.
 */
@SpringBootApplication
public class BusinessCockpitStandaloneApplication {

    public static void main(String... args) {

        final var app = new SpringApplication(BusinessCockpitStandaloneApplication.class);
        app.setDefaultProperties(
                Map.of("spring.profiles.default", "local"));
        app.run(args);

    }

}
