package io.vanillabp.cockpit.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Makes {@code @Scheduled} count, which several parts of the cockpit are built on.
 * <p>
 * Switching it off stops the cockpit's own scheduled work, so the property exists for an application
 * which schedules on its own terms and knows what it gives up. The startup line says as much.
 */
@AutoConfiguration
@ConditionalOnBooleanProperty(name = "business-cockpit.scheduling.enabled", matchIfMissing = true)
@EnableScheduling
public class BusinessCockpitSchedulingAutoConfiguration {

    private static final Logger logger =
            LoggerFactory.getLogger(BusinessCockpitSchedulingAutoConfiguration.class);

    public BusinessCockpitSchedulingAutoConfiguration() {

        new WholeApplicationSwitch(
                "@EnableScheduling",
                "every @Scheduled method of this application runs on Spring's scheduler. The cockpit's "
                        + "own scheduled work runs there too, among it the notification poller and the "
                        + "collector which pushes live updates to the browser.",
                "set 'business-cockpit.scheduling.enabled: false', and expect that work of the cockpit "
                        + "to stop with it.")
                .reportTo(logger);

    }

}
