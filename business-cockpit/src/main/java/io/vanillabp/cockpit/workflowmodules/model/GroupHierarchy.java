package io.vanillabp.cockpit.workflowmodules.model;

import java.util.List;

/**
 * One entry of a workflow module's group hierarchy, as it is stored in the module's document.
 *
 * @param group The group
 * @param targets The groups a member of {@code group} is a member of as well
 */
public record GroupHierarchy(
                             String group,
                             List<String> targets) {
}
