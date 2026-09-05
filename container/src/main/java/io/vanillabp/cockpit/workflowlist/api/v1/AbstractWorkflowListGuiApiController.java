package io.vanillabp.cockpit.workflowlist.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.UserContext;
import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
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
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

public abstract class AbstractWorkflowListGuiApiController implements OfficialWorkflowlistApi {

    @Autowired
    protected UserContext userContext;

    @Autowired
    protected GuiApiMapper mapper;

    @Autowired
    protected io.vanillabp.cockpit.tasklist.api.v1.GuiApiMapper userTaskMapper;

    protected abstract Page<Workflow> getWorkflows(
            final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
            final int pageNumber,
            final int pageSize,
            final OffsetDateTime initialTimestamp,
            final List<String> businessIds,
            final List<SearchQuery> searchQueries,
            final String sort,
            final boolean sortAscending,
            final WorkflowlistService.RetrieveItemsMode mode);

    @Override
    public ResponseEntity<Workflows> getWorkflows(
            final WorkflowsRequest workflowsRequest,
            final String requestId,
            final OffsetDateTime initialTimestamp) {

        if (workflowsRequest == null) {
            return ResponseEntity.badRequest().build();
        }

        final var timestamp = initialTimestamp != null
                ? initialTimestamp
                : OffsetDateTime.now();

        final var currentUser = userContext.getUserLoggedInDetails();

        final var workflows = getWorkflows(
                currentUser,
                workflowsRequest.getPageNumber(),
                workflowsRequest.getPageSize(),
                timestamp,
                workflowsRequest.getBusinessIds(),
                mapper.toModel(workflowsRequest.getSearchQueries()),
                workflowsRequest.getSort(),
                workflowsRequest.getSortAscending(),
                workflowsRequest.getMode() != null
                        ? mapper.toModel(workflowsRequest.getMode())
                        : WorkflowlistService.RetrieveItemsMode.All);

        return ResponseEntity.ok(mapper.toApi(workflows, timestamp, requestId));

    }

    protected abstract Page<Workflow> getWorkflowsUpdated(
            final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
            final int size,
            final Collection<String> knownWorkflowsIds,
            final OffsetDateTime initialTimestamp,
            final List<SearchQuery> searchQueries,
            final String sort,
            final boolean sortAscending,
            final WorkflowlistService.RetrieveItemsMode mode);

    @Override
    public ResponseEntity<Workflows> getWorkflowsUpdate(
            final WorkflowsUpdateRequest workflowsUpdateRequest,
            final String requestId,
            final OffsetDateTime initialTimestamp) {

        final var timestamp = initialTimestamp != null
                ? initialTimestamp
                : OffsetDateTime.now();

        final var currentUser = userContext.getUserLoggedInDetails();

        final var workflows = getWorkflowsUpdated(
                currentUser,
                workflowsUpdateRequest.getSize(),
                workflowsUpdateRequest.getKnownWorkflowsIds(),
                timestamp,
                mapper.toModel(workflowsUpdateRequest.getSearchQueries()),
                workflowsUpdateRequest.getSort(),
                workflowsUpdateRequest.getSortAscending(),
                workflowsUpdateRequest.getMode() != null
                        ? mapper.toModel(workflowsUpdateRequest.getMode())
                        : WorkflowlistService.RetrieveItemsMode.Active);

        return ResponseEntity.ok(mapper.toApi(workflows, timestamp, requestId));

    }

    protected abstract io.vanillabp.cockpit.workflowlist.model.Workflow getWorkflow(
            final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
            final String workflowId);

    @Override
    public ResponseEntity<io.vanillabp.cockpit.gui.api.v1.Workflow> getWorkflow(
            final String workflowId) {

        final var currentUser = userContext.getUserLoggedInDetails();

        final var workflow = getWorkflow(currentUser, workflowId);

        return workflow == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(mapper.toApi(workflow));

    }

    protected abstract List<io.vanillabp.cockpit.tasklist.model.UserTask> getUserTasksOfWorkflow(
            final String workflowId,
            final boolean activeOnlyRequested,
            final boolean limitListAccordingToCurrentUsersPermissions,
            final String currentUser,
            final Collection<String> currentUserGroups,
            final int pageSize,
            final String sort,
            final boolean sortAscending);

    @Override
    public ResponseEntity<List<UserTask>> getUserTasksOfWorkflow(
            final String workflowId,
            final Boolean llatcup,
            final UserTasksRequest userTasksRequest) {

        final var currentUser = userContext.getUserLoggedInDetails();

        final var userTasks = getUserTasksOfWorkflow(
                workflowId,
                userTasksRequest.getMode() == UserTaskRetrieveMode.OPENTASKS,
                llatcup != null ? llatcup : true,
                currentUser.getId(),
                currentUser.getAuthorities(),
                userTasksRequest.getPageSize() == null ? 100 : userTasksRequest.getPageSize(),
                userTasksRequest.getSort(),
                userTasksRequest.getSortAscending() == null || userTasksRequest.getSortAscending())
                .stream()
                .map(userTask -> userTaskMapper.toApi(userTask, currentUser.getId()))
                .toList();

        return ResponseEntity.ok(userTasks);

    }

    protected abstract List<io.vanillabp.cockpit.util.kwic.KwicResult> kwic(
            final UserDetails currentUser,
            final OffsetDateTime endedSince,
            final List<SearchQuery> searchQueries,
            final String path,
            final String query);

    @Override
    public ResponseEntity<KwicResults> getKwicResults(
            final KwicRequest kwicRequest,
            final OffsetDateTime initialTimestamp,
            final String path,
            final String query) {

        final var effectivePath = StringUtils.hasText(path)
                ? path
                : "detailsFulltextSearch";

        final var timestamp = initialTimestamp != null
                ? initialTimestamp
                : OffsetDateTime.now();

        final var currentUser = userContext.getUserLoggedInDetails();

        final var searchQueries = Optional
                .ofNullable(kwicRequest.getSearchQueries())
                .orElse(List.of())
                .stream()
                .map(mapper::toModel)
                .toList();

        final var result = kwic(currentUser, timestamp, searchQueries, effectivePath, query)
                .stream()
                .map(mapper::toApi)
                .toList();

        return ResponseEntity.ok(new KwicResults().result(result));

    }

}
