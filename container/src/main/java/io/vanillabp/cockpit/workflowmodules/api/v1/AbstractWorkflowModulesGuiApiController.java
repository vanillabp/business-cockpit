package io.vanillabp.cockpit.workflowmodules.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.commons.security.usercontext.reactive.ReactiveUserContext;
import io.vanillabp.cockpit.gui.api.v1.OfficialWorkflowModulesApi;
import io.vanillabp.cockpit.gui.api.v1.WorkflowModule;
import io.vanillabp.cockpit.gui.api.v1.WorkflowModules;
import io.vanillabp.cockpit.workflowmodules.WorkflowModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public abstract class AbstractWorkflowModulesGuiApiController implements OfficialWorkflowModulesApi {

    @Autowired
    protected WorkflowModuleService service;

    @Autowired
    protected GuiApiMapper mapper;

    @Autowired
    protected ReactiveUserContext userContext;

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

        return userContext.getUserLoggedInDetailsAsMono().flatMap(userDetails ->
                        getWorkflowMoules(userDetails)
                        .map(mapper::toApi)
                        .collectList()
                        .map(modules -> new WorkflowModules().modules(modules))
                        .map(ResponseEntity::ok)
                );
    }

    protected abstract Flux<io.vanillabp.cockpit.workflowmodules.model.WorkflowModule> getWorkflowMoules(
            UserDetails userDetails);

}
