package io.vanillabp.cockpit.config.startup;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Keeps the BPMS API out of the application until realm, user and password are configured, so that
 * an application which has not set them yet still boots. {@link StartupConfigurationCheck} says so
 * in a warning on every start.
 * <p>
 * A hand-written condition rather than {@code @ConditionalOnProperty}: that one counts a property
 * set to an empty string as present, which would let the incomplete configuration through to the
 * very exceptions this replaces.
 */
public class BpmsApiIsConfigured implements Condition {

    @Override
    public boolean matches(
            final ConditionContext context,
            final AnnotatedTypeMetadata metadata) {

        return CockpitConfiguration.isBpmsApiConfigured(context.getEnvironment());

    }

}
