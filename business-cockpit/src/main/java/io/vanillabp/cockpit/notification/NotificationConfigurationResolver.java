package io.vanillabp.cockpit.notification;

import io.vanillabp.cockpit.notification.model.NotificationConfiguration;
import io.vanillabp.cockpit.notification.model.WorkflowNotificationConfiguration;
import java.util.Map;

/**
 * Answers one question off a {@link NotificationConfiguration} tree: does this user want to be
 * notified about this workflow through this medium? It reads and changes nothing.
 * <p>
 * The rules:
 * <ol>
 *   <li>A configuration which is absent or empty means "none", so the user is not notified.</li>
 *   <li>A setting for one workflow wins over the setting for all of them.</li>
 *   <li>Choosing "none", for all workflows or for one, switches every medium off for that
 *       scope.</li>
 *   <li>The media do not depend on each other.</li>
 * </ol>
 * The notification poller uses it, and it is unit-tested on its own.
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
                // "none" for a workflow clears every medium of that scope
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
