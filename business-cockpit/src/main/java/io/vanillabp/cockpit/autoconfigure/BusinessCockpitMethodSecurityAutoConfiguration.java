package io.vanillabp.cockpit.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Makes {@code @Secured} count, which is how the BPMS API keeps its endpoints to the one role its
 * client is given.
 * <p>
 * The switch is a property and not a missing bean. What {@code @EnableMethodSecurity} registers
 * carries no bean name Spring Security publishes, so a library cannot ask whether the application
 * has already asked for method security. The property is the answer which does not depend on
 * guessing.
 */
@AutoConfiguration
@ConditionalOnBooleanProperty(name = "business-cockpit.method-security.enabled", matchIfMissing = true)
@EnableMethodSecurity(securedEnabled = true)
public class BusinessCockpitMethodSecurityAutoConfiguration {

    private static final Logger logger =
            LoggerFactory.getLogger(BusinessCockpitMethodSecurityAutoConfiguration.class);

    public BusinessCockpitMethodSecurityAutoConfiguration() {

        new WholeApplicationSwitch(
                "@EnableMethodSecurity(securedEnabled = true)",
                "@Secured is enforced on every bean of this application, yours included. The cockpit "
                        + "needs it for the BPMS API, whose controllers carry @Secured.",
                "set 'business-cockpit.method-security.enabled: false' and write @EnableMethodSecurity "
                        + "yourself. Without it the BPMS API is left with what its own security chain "
                        + "protects.")
                .reportTo(logger);

    }

}
