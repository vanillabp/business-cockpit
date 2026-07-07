package io.vanillabp.cockpit.notification.model;

import java.util.Map;

/**
 * The complete per-user notification configuration (stored as an object tree on the {@link User}
 * document, AC tech 7).
 * <p>
 * The default (an absent or empty configuration) means "none": the user is not notified. Enabling
 * a medium globally means "all via &lt;medium&gt;". Per-workflow entries override the global setting
 * for individual workflows (AC func 4).
 *
 * @param globalAllViaMedium per-medium global setting, keyed by the medium type (e.g. {@code "email"});
 *                           {@code true} = "all via that medium". Absence/{@code false} = "none".
 * @param perWorkflow        per-workflow overrides, keyed by {@code workflowModuleId + "#" + bpmnProcessId}
 */
public record NotificationConfiguration(
        Map<String, Boolean> globalAllViaMedium,
        Map<String, WorkflowNotificationConfiguration> perWorkflow) {

    /**
     * Builds the per-workflow key used in {@link #perWorkflow()}.
     */
    public static String workflowKey(
            final String workflowModuleId,
            final String bpmnProcessId) {

        return workflowModuleId + "#" + bpmnProcessId;

    }

}
