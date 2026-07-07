package io.vanillabp.cockpit.notification.model;

import java.util.Map;

/**
 * Per-workflow override of the global notification setting (AC func 4b/4c).
 * <p>
 * Depending on the global setting this either enables individual media for a workflow (when the
 * global setting is "none") or excludes them (when the global setting is "all via &lt;medium&gt;").
 *
 * @param none          if {@code true}, this workflow is never notified, regardless of medium
 *                      (selecting "none" clears all "all via &lt;medium&gt;" options for this scope,
 *                      AC func 4e)
 * @param allViaMedium  per-medium override, keyed by the medium type (e.g. {@code "email"}); a value
 *                      overrides the global setting for that medium and workflow. Absent entries fall
 *                      back to the global setting.
 */
public record WorkflowNotificationConfiguration(
        boolean none,
        Map<String, Boolean> allViaMedium) {

}
