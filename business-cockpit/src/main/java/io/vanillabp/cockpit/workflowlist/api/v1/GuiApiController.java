package io.vanillabp.cockpit.workflowlist.api.v1;

import io.vanillabp.cockpit.gui.api.v1.GuiEvent;
import io.vanillabp.cockpit.gui.api.v1.Page;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.gui.api.v1.Workflow;
import io.vanillabp.cockpit.gui.api.v1.WorkflowEvent;
import io.vanillabp.cockpit.gui.api.v1.WorkflowlistApi;
import io.vanillabp.cockpit.gui.api.v1.Workflows;
import io.vanillabp.cockpit.gui.api.v1.WorkflowsUpdate;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.workflowlist.WorkflowChangedNotification;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@RestController("workflowListGuiApiController")
@RequestMapping(path = "/gui/api/v1")
public class GuiApiController implements WorkflowlistApi {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private WorkflowlistService workflowlistService;
    
    @Autowired
    private UserTaskService userTaskService;

    @Autowired
    private GuiApiMapper mapper;
    
    @Autowired
    private io.vanillabp.cockpit.tasklist.api.v1.GuiApiMapper userTaskMapper;

    @EventListener(classes = WorkflowChangedNotification.class)
    public void updateClients(
            final WorkflowChangedNotification notification) {

        applicationEventPublisher.publishEvent(
                new GuiEvent(
                        notification.getSource(),
                        notification.getTargetRoles(),
                        new WorkflowEvent()
                                .name("Workflow")
                                .id(notification.getWorkflowId())
                                .type(notification.getType().toString())));

    }

    @Override
    public Mono<ResponseEntity<Workflows>> getWorkflows(
            final Integer pageNumber,
            final Integer pageSize,
            final ServerWebExchange exchange) {

        final var workflows = workflowlistService.getWorkflows(
                pageNumber,
                pageSize);

        return workflows.map(page -> ResponseEntity.ok(
                new Workflows()
                        .page(new Page()
                                .number(page.getNumber())
                                .size(page.getSize())
                                .totalElements(page.getTotalElements())
                                .totalPages(page.getTotalPages()))
                        .workflows(mapper.toApi(page.getContent()))
                        .serverTimestamp(OffsetDateTime.now())));

    }

    @Override
    public Mono<ResponseEntity<Workflows>> getWorkflowsUpdate(
            final Mono<WorkflowsUpdate> workflowsUpdate,
            final ServerWebExchange exchange) {

        return workflowsUpdate
                .flatMap(update -> workflowlistService.getWorkflowsUpdated(
                        update.getSize(),
                        update.getKnownWorkflowIds()))
                .map(page -> ResponseEntity.ok(
                        new Workflows()
                                .page(new Page()
                                        .number(page.getNumber())
                                        .size(page.getSize())
                                        .totalElements(page.getTotalElements())
                                        .totalPages(page.getTotalPages()))
                                .workflows(mapper.toApi(page.getContent()))
                                .serverTimestamp(OffsetDateTime.now())))
                .switchIfEmpty(Mono.just(ResponseEntity.badRequest().build()));

    }
    
    @Override
    public Mono<ResponseEntity<Workflow>> getWorkflow(
            final String workflowId,
            final ServerWebExchange exchange) {
        
        return workflowlistService
                .getWorkflow(workflowId)
                .map(mapper::toApi)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
        
    }
    
    @Override
    public Mono<ResponseEntity<Flux<UserTask>>> getUserTasksOfWorkflow(
            final String workflowId,
            final Boolean activeOnlyRequested,
            final Boolean llatcup,
            final ServerWebExchange exchange) {
        
        final var activeOnly = activeOnlyRequested != null ? activeOnlyRequested.booleanValue() : true;
        
        return Mono.just(
                ResponseEntity.ok(
                        userTaskService
                                .getUserTasksOfWorkflow(activeOnly, workflowId)
                                .map(t -> userTaskMapper.toApi(t))));
        
    }
    
}
