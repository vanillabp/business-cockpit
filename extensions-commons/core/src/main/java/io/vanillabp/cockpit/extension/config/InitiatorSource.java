package io.vanillabp.cockpit.extension.config;

/**
 * Who answers the initiator of a report, which is the user who caused what is reported.
 * <p>
 * No workflow system records that user, and the extension cannot find it out either: a report is
 * built at the moment of the event, and the security context of the request which caused it is
 * gone by then. So the application answers, or it says that this workflow module knows no
 * user-triggered action at all. The key is named after where the value comes from and not after
 * the value, because what it settles is who answers.
 * <p>
 * The property has no default. An application which reports to the cockpit and says this nowhere
 * does not start. A default would turn a value which was optional in version 1 into an exception
 * without anybody saying so, and the initiator cannot be added to a case afterwards.
 */
public enum InitiatorSource {

  /**
   * The application sets the initiator in a method annotated with
   * <code>&#64;WorkflowDetailsProvider</code> or <code>&#64;UserTaskDetailsProvider</code>. A
   * report which carries none after the provider ran fails.
   */
  BY_APPLICATION("by-application"),

  /**
   * This workflow module knows no action a user caused, so every report of it carries
   * <code>system</code>.
   */
  SYSTEM("system");

  private final String configuredAs;

  InitiatorSource(
      final String configuredAs) {

    this.configuredAs = configuredAs;

  }

  /**
   * @return The text an application writes into the property
   */
  public String configuredAs() {

    return configuredAs;

  }

  /**
   * Reads a configured value.
   *
   * @param value What was configured
   * @return The source
   * @throws IllegalArgumentException If the value names neither of the two. The message lists
   *           both
   */
  public static InitiatorSource of(
      final String value) {

    for (final var candidate : values()) {
      if (candidate.configuredAs.equals(value)) {
        return candidate;
      }
    }
    throw new IllegalArgumentException(
        """
            '%s' is neither of the two values this key takes! Write '%s' where the application \
            sets the initiator itself, in a method annotated with @WorkflowDetailsProvider or \
            @UserTaskDetailsProvider, or '%s' where this workflow module knows no action a user \
            causes."""
            .formatted(value, BY_APPLICATION.configuredAs, SYSTEM.configuredAs));

  }

}
