package io.vanillabp.cockpit;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The runnable business cockpit: the library {@link BusinessCockpitApplication} plus what turns it
 * into an application anybody can start. Extending the library class is the same step a custom
 * cockpit takes, which is why this class is the example the documentation points at.
 * <p>
 * Beyond starting Spring Boot this module adds the concrete GUI API controllers, the demo users of
 * the profile {@code local} and the {@code application*.yaml} defaults below {@code config/}.
 */
@SpringBootApplication
public class BusinessCockpitStandaloneApplication extends BusinessCockpitApplication {

    public static void main(String... args) {

        final var app = new SpringApplication(BusinessCockpitStandaloneApplication.class);
        app.setDefaultProperties(
                Map.of("spring.profiles.default", "local"));
        app.run(args);

    }

}
