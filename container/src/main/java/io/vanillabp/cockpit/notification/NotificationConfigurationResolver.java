package io.vanillabp.cockpit.notification;

import io.vanillabp.cockpit.notification.model.NotificationConfiguration;
import io.vanillabp.cockpit.notification.model.WorkflowNotificationConfiguration;
import java.util.Map;

/**
 * Pure, side-effect-free resolution of "does the user want to be notified for this
 * workflow via this medium?" from a {@link NotificationConfiguration} object tree.
 * <p>
 * Rules (AC func 4):
 * <ol>
 *   <li>The default (no/empty configuration) is "none" - the user is not notified.</li>
 *   <li>A per-workflow override wins over the global setting.</li>
 *   <li>Selecting "none" (globally or per-workflow) turns all media off for that scope
 *       (AC func 4e).</li>
 *   <li>Media are independent of each other.</li>
 * </ol>
 * Reused by the notification poller (T04) and unit-tested in isolation.
 */
public final class NotificationConfigurationResolver {

    private NotificationConfigurationResolver() {
    }

    /**
     * @param configuration    the user's configuration (may be {@code null})
     * @param workflowModuleId the workflow module of the user task
     * @param bpmnProcessId    the BPMN process id of the user task
     * @param mediumType       the medium type (e.g. {@code "email"})
     * @return {@code true} if the user wants to be notified for this workflow via this medium
     */
    public static boolean shouldNotify(
            final NotificationConfiguration configuration,
            final String workflowModuleId,
            final String bpmnProcessId,
            final String mediumType) {

        if (configuration == null) {
            return false;
        }

        final var perWorkflow = configuration.perWorkflow();
        if (perWorkflow != null) {
            final WorkflowNotificationConfiguration override =
                    perWorkflow.get(NotificationConfiguration.workflowKey(workflowModuleId, bpmnProcessId));
            if (override != null) {
                // "none" for a workflow clears all media for that scope (AC func 4e).
                if (override.none()) {
                    return false;
                }
                final Map<String, Boolean> allViaMedium = override.allViaMedium();
                if (allViaMedium != null) {
                    final Boolean perMedium = allViaMedium.get(mediumType);
                    if (perMedium != null) {
                        // explicit per-workflow override for this medium wins over the global setting
                        return perMedium;
                    }
                }
                // no explicit per-medium override for this workflow -> fall through to the global setting
            }
        }

        final Map<String, Boolean> globalAllViaMedium = configuration.globalAllViaMedium();
        return globalAllViaMedium != null
                && Boolean.TRUE.equals(globalAllViaMedium.get(mediumType));

    }

}
