package io.vanillabp.cockpit.workflowlist.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.reactive.ReactiveUserContext;
import io.vanillabp.cockpit.gui.api.v1.GuiEvent;
import io.vanillabp.cockpit.gui.api.v1.KwicRequest;
import io.vanillabp.cockpit.gui.api.v1.KwicResults;
import io.vanillabp.cockpit.gui.api.v1.OfficialWorkflowlistApi;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.gui.api.v1.Workflow;
import io.vanillabp.cockpit.gui.api.v1.WorkflowEvent;
import io.vanillabp.cockpit.gui.api.v1.Workflows;
import io.vanillabp.cockpit.gui.api.v1.WorkflowsRequest;
import io.vanillabp.cockpit.gui.api.v1.WorkflowsUpdateRequest;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.workflowlist.WorkflowChangedNotification;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@RestController("workflowListGuiApiController")
@RequestMapping(path = "/gui/api/v1")
public class GuiApiController implements OfficialWorkflowlistApi {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private WorkflowlistService workflowlistService;
    
    @Autowired
    private UserTaskService userTaskService;

    @Autowired
    private ReactiveUserContext userContext;

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
            final Mono<WorkflowsRequest> workflowsRequest,
            final String requestId,
            final ServerWebExchange exchange) {

        if (workflowsRequest == null) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return workflowsRequest
                .zipWhen(request -> Mono.just(request.getInitialTimestamp() != null
                        ? request.getInitialTimestamp()
                        : OffsetDateTime.now()))
                .flatMap(entry -> Mono.zip(
                        workflowlistService.getWorkflows(
                                entry.getT1().getPageNumber(),
                                entry.getT1().getPageSize(),
                                entry.getT2(),
                                mapper.toModel(entry.getT1().getSearchQueries()),
                                entry.getT1().getSort(),
                                entry.getT1().getSortAscending()),
                        Mono.just(entry.getT2())))
                .map(entry -> mapper.toApi(entry.getT1(), entry.getT2(), requestId))
                .map(ResponseEntity::ok);

    }

    @Override
    public Mono<ResponseEntity<Workflows>> getWorkflowsUpdate(
            final Mono<WorkflowsUpdateRequest> workflowsUpdateRequest,
            final String requestId,
            final ServerWebExchange exchange) {

        return workflowsUpdateRequest
                .zipWhen(update -> Mono.just(update.getInitialTimestamp() != null
                        ? update.getInitialTimestamp()
                        : OffsetDateTime.now()))
                .flatMap(entry -> Mono.zip(
                        workflowlistService.getWorkflowsUpdated(
                                entry.getT1().getSize(),
                                entry.getT1().getKnownWorkflowsIds(),
                                entry.getT2(),
                                mapper.toModel(entry.getT1().getSearchQueries()),
                                entry.getT1().getSort(),
                                entry.getT1().getSortAscending()),
                        Mono.just(entry.getT2())))
                .map(entry -> mapper.toApi(entry.getT1(), entry.getT2(), requestId))
                .map(ResponseEntity::ok)
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
        
        final var activeOnly = activeOnlyRequested == null || activeOnlyRequested;

        return userContext
                .getUserLoggedInAsMono()
                .map(userId -> ResponseEntity.ok(
                        userTaskService
                                .getUserTasksOfWorkflow(activeOnly, workflowId)
                                .map(t -> userTaskMapper.toApi(t, userId))));
        
    }

    @Override
    public Mono<ResponseEntity<KwicResults>> getKwicResults(
            final Mono<KwicRequest> kwicRequest,
            final String path,
            final String query,
            final ServerWebExchange exchange) {

        final var effectivePath = StringUtils.hasText(path)
                ? path
                : "detailsFulltextSearch";

        return workflowlistService
                .kwic(null, effectivePath, query)
                .map(mapper::toApi)
                .collectList()
                .map(result -> new KwicResults().result(result))
                .map(ResponseEntity::ok);

    }

}
