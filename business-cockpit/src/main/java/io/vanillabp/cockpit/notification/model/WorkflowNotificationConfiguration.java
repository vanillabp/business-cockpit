package io.vanillabp.cockpit.notification.model;

import java.util.Map;

/**
 * What one workflow says instead of the setting which holds for all of them.
 * <p>
 * What it does depends on that setting. Where the setting is "none", it switches single media on
 * for this workflow. Where the setting is "all via a medium", it takes single media away again.
 *
 * @param none          {@code true} means this workflow is never notified, whatever the medium.
 *                      Choosing "none" clears every "all via a medium" option of this scope
 * @param allViaMedium  per-medium override, keyed by the medium type (e.g. {@code "email"}); a value
 *                      overrides the global setting for that medium and workflow. Absent entries fall
 *                      back to the global setting.
 */
public record WorkflowNotificationConfiguration(
        boolean none,
        Map<String, Boolean> allViaMedium) {

}
