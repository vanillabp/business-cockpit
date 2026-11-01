package io.vanillabp.cockpit.config.startup;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Keeps a BPMS API chain built from {@code bpms-api.*} out of the application until realm, user and
 * password are configured, so that an application which has not set them yet still boots. The
 * module {@code container} puts it on its chain. {@link StartupConfigurationCheck} says on every
 * start what is missing.
 * <p>
 * The condition is written by hand and not taken from {@code @ConditionalOnProperty}. That one
 * counts a property set to an empty string as present, which would let an incomplete configuration
 * through to the very exceptions this replaces.
 */
public class BpmsApiIsConfigured implements Condition {

  /**
   * Creates the condition. Spring creates it when it reads the {@code @Conditional} annotation.
   */
  public BpmsApiIsConfigured() {
  }

  @Override
  public boolean matches(
      final ConditionContext context,
      final AnnotatedTypeMetadata metadata) {

    return CockpitConfiguration.isBpmsApiConfigured(context.getEnvironment());

  }

}
