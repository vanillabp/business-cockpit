package io.vanillabp.cockpit.gui.api.v1;

import io.vanillabp.cockpit.mongo.updateinfo.UpdateInformationAware;

/**
 * Who made a change, the way an event of the update stream names it: the kind, and the id of the
 * user where the kind is {@link InitiatorKind#USER}.
 *
 * <p>The cockpit stores who made the latest change as one string. It is the id of a user, or one
 * of two names: {@link UpdateInformationAware#SYSTEM_USER} for the workflow system and
 * {@link UpdateInformationAware#COCKPIT_USER} for the cockpit itself. The event does not hand
 * these names to the browser. It says the kind in a field of its own, so a page never mistakes
 * them for a user.
 *
 * <p>Why an event names who made the change, and where the cockpit reads it: see decision 102 in
 * the repository's DECISIONS.md.
 *
 * @param id The id of the user, or {@code null} unless the kind is {@link InitiatorKind#USER}
 * @param kind Who made the change
 */
public record InitiatorOfAChange(
                                 String id,
                                 InitiatorKind kind) {

  /** A change the cockpit itself made, a job of the cockpit for example. */
  public static final InitiatorOfAChange COCKPIT = new InitiatorOfAChange(null, InitiatorKind.COCKPIT);

  /** A change the workflow system made, with no user behind it. */
  public static final InitiatorOfAChange SYSTEM = new InitiatorOfAChange(null, InitiatorKind.SYSTEM);

  /**
   * Reads the string the cockpit stores. Nothing stored counts as the workflow system. Only a
   * report can leave it empty, from a workflow module which does not say who made the change.
   *
   * @param stored The id of a user, one of the two names, or {@code null}
   * @return Who made the change
   */
  public static InitiatorOfAChange fromStored(
      final String stored) {

    if ((stored == null) || stored.isBlank() || UpdateInformationAware.SYSTEM_USER.equals(stored)) {
      return SYSTEM;
    }
    if (UpdateInformationAware.COCKPIT_USER.equals(stored)) {
      return COCKPIT;
    }
    return new InitiatorOfAChange(stored, InitiatorKind.USER);

  }

}
