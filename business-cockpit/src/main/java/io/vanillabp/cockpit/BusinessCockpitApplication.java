package io.vanillabp.cockpit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.util.Map;

@SpringBootApplication
@ComponentScan(basePackageClasses = BusinessCockpitApplication.class)
public class BusinessCockpitApplication {

    public static void main(String... args) {

        final var app = new SpringApplication(BusinessCockpitApplication.class);
        app.setDefaultProperties(
                Map.of("spring.profiles.default", "local"));
        app.run(args);
        
    }

}
