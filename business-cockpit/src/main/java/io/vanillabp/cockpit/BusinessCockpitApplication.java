package io.vanillabp.cockpit;

import io.vanillabp.cockpit.bpms.BpmsApiProperties;
import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.cockpit.notification.NotificationProperties;
import io.vanillabp.cockpit.tasklist.OpenedTasksProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * Everything the business cockpit needs to be wired into a Spring Boot application: the component
 * scan across {@code io.vanillabp.cockpit}, the property classes to bind and the Spring features
 * the cockpit code relies on.
 * <p>
 * The class carries no {@code @SpringBootApplication} and no {@code main} method on purpose. It is
 * a library and starting it would say the opposite. An application extends it and adds both:
 * <pre>
 * &#64;SpringBootApplication
 * public class MyCockpit extends BusinessCockpitApplication {
 *     public static void main(String... args) {
 *         SpringApplication.run(MyCockpit.class, args);
 *     }
 * }
 * </pre>
 * Spring reads the annotations below off the superclass while parsing the subclass, so extending is
 * all it takes. {@code io.vanillabp.cockpit.BusinessCockpitStandaloneApplication} of the module
 * {@code container} is such an application and the reference for writing one.
 */
@ComponentScan(basePackageClasses = BusinessCockpitApplication.class)
@EnableConfigurationProperties({
    ApplicationProperties.class,
    BpmsApiProperties.class,
    NotificationProperties.class,
    OpenedTasksProperties.class
})
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@EnableAsync
@EnableScheduling
public class BusinessCockpitApplication {

}
