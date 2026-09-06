package io.vanillabp.cockpit.workflowmodules.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.workflowmodules.model.WorkflowModule;
import java.util.List;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("workflowModulesGuiApiController")
@RequestMapping(path = "/gui/api/v1")
public class GuiApiController extends AbstractWorkflowModulesGuiApiController {

    @Override
    protected List<WorkflowModule> getWorkflowModules(UserDetails userDetails) {
	    return service.getWorkflowModules(userDetails.getAuthorities());
    }
}
