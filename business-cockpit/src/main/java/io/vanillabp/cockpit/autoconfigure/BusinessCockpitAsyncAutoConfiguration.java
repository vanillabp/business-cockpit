package io.vanillabp.cockpit.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Makes {@code @Async} count.
 * <p>
 * The cockpit has no {@code @Async} method of its own, so nothing of it changes when this is off.
 * The annotation is here because a cockpit application used to inherit it from the base class, and
 * an application which relies on it would otherwise find its asynchronous methods running on the
 * calling thread after the upgrade. Which is why the switch is a property: this one is for the
 * application to decide.
 */
@AutoConfiguration
@ConditionalOnBooleanProperty(name = "business-cockpit.async.enabled", matchIfMissing = true)
@EnableAsync
public class BusinessCockpitAsyncAutoConfiguration {

    private static final Logger logger =
            LoggerFactory.getLogger(BusinessCockpitAsyncAutoConfiguration.class);

    public BusinessCockpitAsyncAutoConfiguration() {

        new WholeApplicationSwitch(
                "@EnableAsync",
                "every @Async method of this application returns at once and runs on a task executor. "
                        + "The cockpit has no such method itself, so this is here for your code.",
                "set 'business-cockpit.async.enabled: false'.")
                .reportTo(logger);

    }

}
