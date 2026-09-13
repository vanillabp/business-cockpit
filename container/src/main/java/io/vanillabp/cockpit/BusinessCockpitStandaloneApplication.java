package io.vanillabp.cockpit;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The runnable business cockpit, and the shortest example of an application built on the library.
 * <p>
 * There is no base class and no cockpit annotation here. The dependency on
 * {@code business-cockpit} is what brings the cockpit along, through the auto-configurations of
 * {@code io.vanillabp.cockpit.autoconfigure}, and a custom cockpit writes exactly this much.
 * <p>
 * What this module adds beyond starting Spring Boot is the part a custom cockpit writes for itself as
 * well: the concrete GUI API controllers, the demo users of the profile {@code local} and the
 * {@code application*.yaml} defaults below {@code config/}. They are found by the component scan of
 * {@code @SpringBootApplication}, which reaches the packages below this one.
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
