package io.vanillabp.cockpit.workflowmodules.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.workflowmodules.WorkflowModuleVisibility;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The module list of the delivered application, holding the modules registered for one of the
 * user's groups and the modules which registered no group at all.
 */
@RestController("workflowModulesGuiApiController")
@RequestMapping(path = "/gui/api/v1")
public class GuiApiController extends AbstractWorkflowModulesGuiApiController {

    @Override
    protected WorkflowModuleVisibility workflowModulesVisibleTo(
            final UserDetails currentUser) {

        return WorkflowModuleVisibility.modulesAddressedTo(currentUser);

    }

}
