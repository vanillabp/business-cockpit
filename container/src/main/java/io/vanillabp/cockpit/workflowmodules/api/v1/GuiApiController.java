package io.vanillabp.cockpit.workflowmodules.api.v1;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.workflowmodules.WorkflowModuleVisibility;

/**
 * The module list of the delivered application, holding the modules registered for one of the
 * user's groups and the modules which registered no group at all.
 */
@RestController("workflowModulesGuiApiController")
@RequestMapping(path = "/gui/api/v1")
public class GuiApiController extends AbstractWorkflowModulesGuiApiController {

  /**
   * Called by Spring, which finds this controller by its component scan and creates it.
   */
  public GuiApiController() {
  }

  @Override
  protected WorkflowModuleVisibility workflowModulesVisibleTo(
      final UserDetails currentUser) {

    return WorkflowModuleVisibility.modulesAddressedTo(currentUser);

  }

}
