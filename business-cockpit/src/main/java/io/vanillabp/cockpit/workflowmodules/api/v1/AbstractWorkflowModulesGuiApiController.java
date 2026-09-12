package io.vanillabp.cockpit.workflowmodules.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.UserContext;
import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.gui.api.v1.OfficialWorkflowModulesApi;
import io.vanillabp.cockpit.gui.api.v1.WorkflowModule;
import io.vanillabp.cockpit.gui.api.v1.WorkflowModules;
import io.vanillabp.cockpit.workflowmodules.WorkflowModuleService;
import io.vanillabp.cockpit.workflowmodules.WorkflowModuleVisibility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/**
 * Turns the workflow-module requests of the GUI API into calls of {@link WorkflowModuleService} and
 * maps the result back. The one thing it does not decide is which modules this view lets the
 * person making the request reach.
 *
 * <p>A subclass says so in {@link #workflowModulesVisibleTo(UserDetails)} and is then done. The
 * list and the request naming a single module are answered from that one
 * {@link WorkflowModuleVisibility}, so a module kept out of the list is reported as unknown when it
 * is asked for by its id.
 */
public abstract class AbstractWorkflowModulesGuiApiController implements OfficialWorkflowModulesApi {

    @Autowired
    protected WorkflowModuleService service;

    @Autowired
    protected GuiApiMapper mapper;

    @Autowired
    protected UserContext userContext;

    /**
     * The workflow modules this view lets the given user reach, which is the only thing a subclass
     * has to decide.
     */
    protected abstract WorkflowModuleVisibility workflowModulesVisibleTo(
            final UserDetails currentUser);

    @Override
    public ResponseEntity<WorkflowModule> getWorkflowModule(
            final String workflowModuleId) {

        final var visibility = workflowModulesVisibleTo(userContext.getUserLoggedInDetails());

        final var workflowModule = service.getWorkflowModule(visibility, workflowModuleId);

        return workflowModule == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(mapper.toApi(workflowModule));

    }

    @Override
    public ResponseEntity<WorkflowModules> getWorkflowModules() {

        final var visibility = workflowModulesVisibleTo(userContext.getUserLoggedInDetails());

        final var modules = service
                .getWorkflowModules(visibility)
                .stream()
                .map(mapper::toApi)
                .toList();

        return ResponseEntity.ok(new WorkflowModules().modules(modules));

    }

}
