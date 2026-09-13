package io.vanillabp.cockpit.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * Switches Spring Security's servlet support on, which the cockpit's two chains are built with.
 * <p>
 * The condition is the one Spring Boot uses for the same annotation: an application which already
 * has a {@code springSecurityFilterChain}, from its own {@code @EnableWebSecurity} or from a bean
 * definition of any other kind, keeps it. Ordered ahead of Spring Boot's own so that the cockpit is
 * the one reporting the switch rather than a line nobody wrote.
 */
@AutoConfiguration(before = ServletWebSecurityAutoConfiguration.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnMissingBean(name = BeanIds.SPRING_SECURITY_FILTER_CHAIN)
@EnableWebSecurity
public class BusinessCockpitWebSecurityAutoConfiguration {

    private static final Logger logger =
            LoggerFactory.getLogger(BusinessCockpitWebSecurityAutoConfiguration.class);

    public BusinessCockpitWebSecurityAutoConfiguration() {

        new WholeApplicationSwitch(
                "@EnableWebSecurity",
                "Spring Security builds its filter chain, and every request of this application passes it.",
                "write @EnableWebSecurity on your own application class. Spring Boot adds the annotation "
                        + "as well when nobody else does, so a servlet application with Spring Security on "
                        + "its classpath has it either way.")
                .reportTo(logger);

    }

}
