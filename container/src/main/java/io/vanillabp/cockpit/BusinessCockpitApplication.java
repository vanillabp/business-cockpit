package io.vanillabp.cockpit;

import io.vanillabp.cockpit.bpms.BpmsApiProperties;
import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.cockpit.notification.NotificationProperties;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringBootApplication
@ComponentScan(basePackageClasses = BusinessCockpitApplication.class)
@EnableConfigurationProperties({
    ApplicationProperties.class,
    BpmsApiProperties.class,
    NotificationProperties.class
})
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
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
