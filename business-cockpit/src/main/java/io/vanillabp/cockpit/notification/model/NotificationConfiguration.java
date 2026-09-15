package io.vanillabp.cockpit.notification.model;

import java.util.Map;

/**
 * Everything one user configured about notifications. It is stored as a tree on the
 * {@link User} document.
 * <p>
 * A configuration which is absent or empty means "none", so the user is not notified. Switching a
 * medium on for every workflow means "all via that medium". An entry for one workflow wins over
 * that setting for that workflow.
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
