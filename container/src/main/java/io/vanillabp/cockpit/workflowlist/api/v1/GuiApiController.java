package io.vanillabp.cockpit.workflowlist.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.workflowlist.WorkflowVisibility;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The workflow list of the delivered application, holding the workflows which name the user in
 * their accessibleToUsers, the ones naming a group of theirs in accessibleToGroups, and the ones
 * naming neither and therefore open to everybody.
 *
 * <p>The user's groups are the authorities of the current request, which the JWT filter has already
 * widened by everything the registered workflow modules grant through their group hierarchy. The
 * task list of this application reads them from the same place.
 */
@RestController("workflowListGuiApiController")
@RequestMapping(path = "/gui/api/v1")
public class GuiApiController extends AbstractWorkflowListGuiApiController {

    @Override
    protected WorkflowVisibility workflowsVisibleTo(
            final UserDetails currentUser) {

        return WorkflowVisibility.workflowsAddressedTo(currentUser);

    }

}
