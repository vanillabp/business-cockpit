package io.vanillabp.cockpit.extension.spi;

import java.util.EnumSet;
import java.util.Set;

/**
 * One key a BPMS half of the Business Cockpit reads below an adapter id. See
 * {@link AdapterCockpitKeys}.
 *
 * @param name The key below the <code>cockpit</code> section, for example
 *          <code>suspension-check-interval</code>
 * @param readAt The levels at which the half reads the key. A key written at any other level
 *          ends the startup
 * @param example A value which the message about a misplaced key shows, so that a developer can
 *          copy a whole line
 * @param whyOnlyThere One sentence which says why the other levels are not read. It closes the
 *          message about a misplaced key
 */
public record AdapterCockpitKey(
                                String name,
                                Set<Level> readAt,
                                String example,
                                String whyOnlyThere) {

  /**
   * The levels at which a key can be written below an adapter id, from the least specific one to
   * the most specific one.
   */
  public enum Level {

    /** <code>vanillabp.adapters.&lt;adapter-id&gt;.cockpit.&lt;key&gt;</code> */
    ADAPTER,

    /**
     * <code>vanillabp.workflow-modules.&lt;module&gt;.adapters.&lt;adapter-id&gt;.cockpit.&lt;key&gt;</code>
     */
    WORKFLOW_MODULE,

    /** The same below <code>workflows.&lt;bpmn-process-id&gt;</code> of a workflow module. */
    WORKFLOW,

    /**
     * The same below <code>tasks.&lt;task&gt;</code> or <code>user-tasks.&lt;task&gt;</code> of a
     * workflow.
     */
    TASK

  }

  /**
   * Takes a copy of the levels, so a caller cannot change them afterwards.
   */
  public AdapterCockpitKey {
    if ((name == null) || (readAt == null) || readAt.isEmpty()) {
      throw new IllegalArgumentException("A key needs a name and at least one level it is read at");
    }
    readAt = Set.copyOf(EnumSet.copyOf(readAt));
  }

  /**
   * A key which is read at the adapter and nowhere below it.
   *
   * @param name The key below the <code>cockpit</code> section
   * @param example A value to show in a message
   * @param whyOnlyThere Why the levels below the adapter are not read
   * @return The key
   */
  public static AdapterCockpitKey atTheAdapterOnly(
      final String name,
      final String example,
      final String whyOnlyThere) {

    return new AdapterCockpitKey(name, Set.of(Level.ADAPTER), example, whyOnlyThere);

  }

}
