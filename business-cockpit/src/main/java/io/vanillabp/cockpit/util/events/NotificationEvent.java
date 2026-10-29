package io.vanillabp.cockpit.util.events;

import java.time.Clock;
import java.util.Collection;
import java.util.List;

import org.springframework.context.ApplicationEvent;

/**
 * An event which tells the open browsers about a change. It names the users who may be told: only
 * a user who is a member of one of the target groups gets it.
 */
public abstract class NotificationEvent extends ApplicationEvent {

  private static final long serialVersionUID = 1L;

  /** The kind of change. */
  public enum Type {
    /** Something new was stored. */
    INSERT,
    /** Something stored was changed. */
    UPDATE,
    /** Something stored was removed. */
    DELETE,
    /** The follow-up date of a user task was reached. */
    FOLLOWUP
  };

  /** The kind of change. */
  private final Type type;

  /** The groups whose members may be told, or {@code null} for everybody. */
  private Collection<String> targetGroups;

  /**
   * Creates the event with a timestamp taken from the given clock.
   *
   * @param source What the event is about
   * @param clock The clock which gives the timestamp of the event
   * @param type The kind of change
   * @param targetGroups The groups whose members may be told, or {@code null} for everybody
   */
  public NotificationEvent(
      final Object source,
      final Clock clock,
      final Type type,
      final Collection<String> targetGroups) {

    super(source, clock);
    this.type = type;
    this.targetGroups = targetGroups;

  }

  /**
   * Creates the event with the current time as its timestamp.
   *
   * @param source What the event is about
   * @param type The kind of change
   * @param targetGroups The groups whose members may be told, or {@code null} for everybody
   */
  public NotificationEvent(
      final Object source,
      final Type type,
      Collection<String> targetGroups) {

    super(source);
    this.type = type;
    this.targetGroups = targetGroups;

  }

  /**
   * The kind of change.
   *
   * @return The kind of change
   */
  public Type getType() {
    return type;
  }

  /**
   * The groups whose members may be told about the change.
   *
   * @return The target groups, or {@code null} for everybody
   */
  public Collection<String> getTargetGroups() {
    return targetGroups;
  }

  /**
   * Whether a user with the given groups may be told about the change.
   *
   * @param groups The groups of the user
   * @return {@code true} if one of the groups is a target group, or if there are no target groups
   */
  public boolean matchesTargetGroups(
      final List<String> groups) {

    if (targetGroups == null) {
      return true;
    }

    return targetGroups
        .stream()
        .flatMap(targetGroup -> groups.stream().map(targetGroup::equals))
        .anyMatch(hasMatchingGroup -> hasMatchingGroup);

  }

}
