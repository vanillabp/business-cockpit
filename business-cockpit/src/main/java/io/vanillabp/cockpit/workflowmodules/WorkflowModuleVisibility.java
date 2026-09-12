package io.vanillabp.cockpit.workflowmodules;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import java.util.Collection;

/**
 * Which workflow modules one view of the cockpit lets a user reach. The module list is built from
 * it and so is the request naming a single module, so a module kept out of the list cannot be
 * opened by its id either.
 *
 * <p>A module registers the groups it is accessible to, and it is visible to the members of those
 * groups. A module which registered no groups at all is open to everybody. Passing null instead of
 * the user's groups drops the restriction, and {@link #everyWorkflowModule()} is the name for that
 * case.
 *
 * <p>This decides the module list and nothing else. The tasks and workflows of a module carry their
 * own visibility, so a user kept out of a module can still meet what that module reported.
 */
public record WorkflowModuleVisibility(
        Collection<String> accessibleToGroups) {

    /** The modules registered for one of the user's groups, plus the ones open to everybody. */
    public static WorkflowModuleVisibility modulesAddressedTo(
            final UserDetails user) {

        return new WorkflowModuleVisibility(user.getAuthorities());

    }

    /** Every registered workflow module, whichever groups it named. */
    public static WorkflowModuleVisibility everyWorkflowModule() {

        return new WorkflowModuleVisibility(null);

    }

}
