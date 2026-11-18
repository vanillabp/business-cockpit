package io.vanillabp.cockpit.support.rest.httpservice;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Decides {@link ConditionalOnHttpServiceGroupAddress}: whether the key {@code <prefix>.base-url}
 * is set, whatever its value. The value is not read, so a placeholder in it stops nothing here.
 */
class OnHttpServiceGroupAddressCondition extends SpringBootCondition {

  @Override
  public ConditionOutcome getMatchOutcome(
      final ConditionContext context,
      final AnnotatedTypeMetadata metadata) {

    final var attributes = metadata.getAnnotationAttributes(ConditionalOnHttpServiceGroupAddress.class.getName());
    final var prefix = attributes == null ? null : (String) attributes.get("prefix");
    if ((prefix == null) || prefix.isBlank() || !ConfigurationPropertyName.isValid(prefix)) {
      throw new IllegalArgumentException("""
          @ConditionalOnHttpServiceGroupAddress has the prefix '%s', which is no valid property \
          key. Use the prefix of the group's keys, for example 'orders.inventory'."""
          .formatted(prefix));
    }

    final var key = prefix
        + ".base-url";
    return context.getEnvironment().containsProperty(key)
        ? ConditionOutcome.match("'"
            + key
            + "' is set")
        : ConditionOutcome.noMatch("'"
            + key
            + "' is not set");

  }

}
