package io.vanillabp.cockpit.workflowlist.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.commons.security.usercontext.reactive.ReactiveUserContext;
import io.vanillabp.cockpit.gui.api.v1.KwicRequest;
import io.vanillabp.cockpit.gui.api.v1.KwicResults;
import io.vanillabp.cockpit.gui.api.v1.OfficialWorkflowlistApi;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.gui.api.v1.UserTaskRetrieveMode;
import io.vanillabp.cockpit.gui.api.v1.UserTasksRequest;
import io.vanillabp.cockpit.gui.api.v1.Workflows;
import io.vanillabp.cockpit.gui.api.v1.WorkflowsRequest;
import io.vanillabp.cockpit.gui.api.v1.WorkflowsUpdateRequest;
import io.vanillabp.cockpit.util.SearchQuery;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public abstract class AbstractWorkflowListGuiApiController implements OfficialWorkflowlistApi {

    @Autowired
    protected ReactiveUserContext userContext;

    @Autowired
    protected GuiApiMapper mapper;

    @Autowired
    protected io.vanillabp.cockpit.tasklist.api.v1.GuiApiMapper userTaskMapper;

    protected abstract Mono<Page<Workflow>> getWorkflows(
            final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
            final int pageNumber,
            final int pageSize,
            final OffsetDateTime initialTimestamp,
            final List<String> businessIds,
            final List<SearchQuery> searchQueries,
            final String sort,
            final boolean sortAscending);

    @Override
    public Mono<ResponseEntity<Workflows>> getWorkflows(
            final Mono<WorkflowsRequest> workflowsRequest,
            final String requestId,
            final OffsetDateTime initialTimestamp,
            final ServerWebExchange exchange) {

        if (workflowsRequest == null) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        final var timestamp = initialTimestamp != null
                ? initialTimestamp
                : OffsetDateTime.now();

        return Mono.zip(
                        userContext.getUserLoggedInDetailsAsMono(),
                        workflowsRequest)
                .flatMap(entry -> getWorkflows(
                                entry.getT1(),
                                entry.getT2().getPageNumber(),
                                entry.getT2().getPageSize(),
                                timestamp,
                                entry.getT2().getBusinessIds(),
                                mapper.toModel(entry.getT2().getSearchQueries()),
                                entry.getT2().getSort(),
                                entry.getT2().getSortAscending()))
                .map(workflows -> mapper.toApi(workflows, timestamp, requestId))
                .map(ResponseEntity::ok);

    }

    protected abstract Mono<Page<Workflow>> getWorkflowsUpdated(
            final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
            final int size,
            final Collection<String> knownWorkflowsIds,
            final OffsetDateTime initialTimestamp,
            final List<SearchQuery> searchQueries,
            final String sort,
            final boolean sortAscending);

    @Override
    public Mono<ResponseEntity<Workflows>> getWorkflowsUpdate(
            final Mono<WorkflowsUpdateRequest> workflowsUpdateRequest,
            final String requestId,
            final OffsetDateTime initialTimestamp,
            final ServerWebExchange exchange) {

        final var timestamp = initialTimestamp != null
                ? initialTimestamp
                : OffsetDateTime.now();

        return Mono.zip(
                        userContext.getUserLoggedInDetailsAsMono(),
                        workflowsUpdateRequest)
                .flatMap(entry -> getWorkflowsUpdated(
                                entry.getT1(),
                                entry.getT2().getSize(),
                                entry.getT2().getKnownWorkflowsIds(),
                                timestamp,
                                mapper.toModel(entry.getT2().getSearchQueries()),
                                entry.getT2().getSort(),
                                entry.getT2().getSortAscending()))
                .map(ids -> mapper.toApi(ids, timestamp, requestId))
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.badRequest().build()));

    }

    protected abstract Mono<io.vanillabp.cockpit.workflowlist.model.Workflow> getWorkflow(
            final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
            final String workflowId);

    @Override
    public Mono<ResponseEntity<io.vanillabp.cockpit.gui.api.v1.Workflow>> getWorkflow(
            final String workflowId,
            final ServerWebExchange exchange) {

        return userContext
                .getUserLoggedInDetailsAsMono()
                .flatMap(currentUser -> getWorkflow(currentUser, workflowId))
                .map(mapper::toApi)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));

    }

    protected abstract Flux<io.vanillabp.cockpit.tasklist.model.UserTask> getUserTasksOfWorkflow(
            final String workflowId,
            final boolean activeOnlyRequested,
            final boolean limitListAccordingToCurrentUsersPermissions,
            final String currentUser,
            final Collection<String> currentUserGroups,
            final int pageSize,
            final String sort,
            final boolean sortAscending);

    @Override
    public Mono<ResponseEntity<Flux<UserTask>>> getUserTasksOfWorkflow(
            final String workflowId,
            final Boolean llatcup,
            final Mono<UserTasksRequest> userTasksRequest,
            final ServerWebExchange exchange) {

        return Mono
                .zip(userContext.getUserLoggedInDetailsAsMono(), userTasksRequest)
                .map(tuple -> getUserTasksOfWorkflow(
                        workflowId,
                        tuple.getT2().getMode() == UserTaskRetrieveMode.OPENTASKS,
                        llatcup != null ? llatcup : true,
                        tuple.getT1().getId(),
                        tuple.getT1().getAuthorities(),
                        tuple.getT2().getPageSize() == null ? 100 : tuple.getT2().getPageSize(),
                        tuple.getT2().getSort(),
                        tuple.getT2().getSortAscending() == null || tuple.getT2().getSortAscending())
                    .map(t -> userTaskMapper.toApi(t, tuple.getT1().getId())))
                .map(ResponseEntity::ok);

    }

    protected abstract Flux<io.vanillabp.cockpit.util.kwic.KwicResult> kwic(
            final UserDetails currentUser,
            final OffsetDateTime endedSince,
            final List<SearchQuery> searchQueries,
            final String path,
            final String query);

    @Override
    public Mono<ResponseEntity<KwicResults>> getKwicResults(
            final Mono<KwicRequest> kwicRequest,
            final OffsetDateTime initialTimestamp,
            final String path,
            final String query,
            final ServerWebExchange exchange) {

        final var effectivePath = StringUtils.hasText(path)
                ? path
                : "detailsFulltextSearch";

        final var timestamp = initialTimestamp != null
                ? initialTimestamp
                : OffsetDateTime.now();

        return Mono.zip(
                        userContext.getUserLoggedInDetailsAsMono(),
                        kwicRequest)
                .flatMapMany(entry -> {
                            final var searchQueries = Optional.ofNullable(
                                            entry.getT2().getSearchQueries())
                                    .orElse(List.of())
                                    .stream()
                                    .map(mapper::toModel)
                                    .toList();
                            return kwic(entry.getT1(), timestamp, searchQueries, effectivePath, query);
                        })
                .map(mapper::toApi)
                .collectList()
                .map(result -> new KwicResults().result(result))
                .map(ResponseEntity::ok);

    }

}
