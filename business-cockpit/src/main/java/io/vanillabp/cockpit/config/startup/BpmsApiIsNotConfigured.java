package io.vanillabp.cockpit.config.startup;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * The counterpart of {@link BpmsApiIsConfigured}, for the one bean which exists only while the
 * BPMS API is switched off. It is a condition of its own and not {@code @ConditionalOnMissingBean},
 * because that one would depend on the order the two bean definitions happen to be read in.
 */
public class BpmsApiIsNotConfigured implements Condition {

    @Override
    public boolean matches(
            final ConditionContext context,
            final AnnotatedTypeMetadata metadata) {

        return !CockpitConfiguration.isBpmsApiConfigured(context.getEnvironment());

    }

}
