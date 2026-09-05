package io.vanillabp.cockpit.workflowmodules.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.UserContext;
import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.gui.api.v1.OfficialWorkflowModulesApi;
import io.vanillabp.cockpit.gui.api.v1.WorkflowModule;
import io.vanillabp.cockpit.gui.api.v1.WorkflowModules;
import io.vanillabp.cockpit.workflowmodules.WorkflowModuleService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

public abstract class AbstractWorkflowModulesGuiApiController implements OfficialWorkflowModulesApi {

    @Autowired
    protected WorkflowModuleService service;

    @Autowired
    protected GuiApiMapper mapper;

    @Autowired
    protected UserContext userContext;

    @Override
    public ResponseEntity<WorkflowModule> getWorkflowModule(
            final String workflowModuleId) {

        final var workflowModule = service.getWorkflowModule(workflowModuleId);

        return workflowModule == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(mapper.toApi(workflowModule));

    }

    @Override
    public ResponseEntity<WorkflowModules> getWorkflowModules() {

        final var modules = getWorkflowModules(userContext.getUserLoggedInDetails())
                .stream()
                .map(mapper::toApi)
                .toList();

        return ResponseEntity.ok(new WorkflowModules().modules(modules));

    }

    protected abstract List<io.vanillabp.cockpit.workflowmodules.model.WorkflowModule> getWorkflowModules(
            UserDetails userDetails);

}
