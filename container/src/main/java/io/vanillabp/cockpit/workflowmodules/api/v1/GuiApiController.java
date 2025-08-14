package io.vanillabp.cockpit.workflowmodules.api.v1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import io.vanillabp.cockpit.commons.security.usercontext.reactive.ReactiveUserContext;
import io.vanillabp.cockpit.gui.api.v1.OfficialWorkflowModulesApi;
import io.vanillabp.cockpit.gui.api.v1.WorkflowModule;
import io.vanillabp.cockpit.gui.api.v1.WorkflowModules;
import io.vanillabp.cockpit.workflowmodules.WorkflowModuleService;
import reactor.core.publisher.Mono;

@RestController("workflowModulesGuiApiController")
@RequestMapping(path = "/gui/api/v1")
public class GuiApiController implements OfficialWorkflowModulesApi {

    @Autowired
    private WorkflowModuleService service;

    @Autowired
    private GuiApiMapper mapper;

    @Autowired
    private ReactiveUserContext userContext;

    @Override
    public Mono<ResponseEntity<WorkflowModule>> getWorkflowModule(
            final String workflowModuleId,
            final ServerWebExchange exchange) {

        return service
                .getWorkflowModule(workflowModuleId)
                .map(mapper::toApi)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));

    }

    @Override
    public Mono<ResponseEntity<WorkflowModules>> getWorkflowModules(
            final ServerWebExchange exchange) {

        return userContext.getUserLoggedInDetailsAsMono().flatMap(entry ->
                    service
                        .getWorkflowModules(entry.getAuthorities())
                        .map(mapper::toApi)
                        .collectList()
                        .map(modules -> new WorkflowModules().modules(modules))
                        .map(ResponseEntity::ok)
                );
    }

}
