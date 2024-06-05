package io.vanillabp.cockpit;

import io.vanillabp.cockpit.bpms.BpmsApiProperties;
import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;

@SpringBootApplication
@ComponentScan(basePackageClasses = BusinessCockpitApplication.class)
@EnableConfigurationProperties({
    ApplicationProperties.class,
    BpmsApiProperties.class
})
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@EnableAsync
@EnableScheduling
public class BusinessCockpitApplication {

    public static void main(String... args) {

        final var app = new SpringApplication(BusinessCockpitApplication.class);
        app.setDefaultProperties(
                Map.of("spring.profiles.default", "local"));
        app.run(args);
        
    }

}
