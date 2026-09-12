package io.vanillabp.cockpit.workflowlist;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import java.util.Collection;
import java.util.List;

/**
 * Which workflows one view of the cockpit lets a user reach. The list of that view is built from
 * it, and so are the requests naming a single workflow, so an id the list would not have shown is
 * answered as unknown rather than opening a way past the list.
 *
 * <p>A workflow is visible once it names one of the {@code accessibleToUsers} or one of the
 * {@code accessibleToGroups}. A collection left null or empty drops its reason instead of widening
 * the view. A visibility naming nobody restricts nothing, and {@link #everyWorkflow()} is the name
 * for that case. {@code includeDanglingWorkflows} decides what happens to
 * a workflow which names neither users nor groups: set, it counts as open to everybody.
 *
 * <p>The values a workflow is matched against are what its {@code @WorkflowDetailsProvider}
 * reported, so an application built on the cockpit answers here with whatever it told the cockpit
 * back then.
 */
public record WorkflowVisibility(
        boolean includeDanglingWorkflows,
        Collection<String> accessibleToUsers,
        Collection<String> accessibleToGroups) {

    /**
     * The workflows addressed to the user, either by name or through one of their groups, plus the
     * ones which address nobody and are therefore open to everybody.
     */
    public static WorkflowVisibility workflowsAddressedTo(
            final UserDetails user) {

        return new WorkflowVisibility(true, List.of(user.getId()), user.getAuthorities());

    }

    /** Every workflow, whoever it addresses. */
    public static WorkflowVisibility everyWorkflow() {

        return new WorkflowVisibility(false, null, null);

    }

}
