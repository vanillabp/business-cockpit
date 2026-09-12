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
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.util.SearchQuery;
import io.vanillabp.cockpit.workflowlist.WorkflowVisibility;
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

/**
 * Turns the workflowlist requests of the GUI API into calls of {@link WorkflowlistService} and maps
 * the result back. One question stays open, and the application built on this library answers it:
 * which workflows this view lets the person making the request reach. Only that application knows
 * what its workflows are about. A cockpit for a support team may
 * want every workflow in one list, another one may show nobody anything the reporting workflow
 * module did not address to them.
 *
 * <p>A subclass answers it in {@link #workflowsVisibleTo(UserDetails)} and is then done. The list,
 * the fulltext suggestions, opening one workflow and asking for its user tasks are answered from
 * that one {@link WorkflowVisibility}, so a workflow kept out of the list cannot be opened by
 * guessing its id, and its tasks do not tell anybody about it either. Both answer as if the id had
 * never been reported.
 *
 * <p>The reference application lets a user see the workflows addressed to them:
 *
 * <pre>
 * &#64;Override
 * protected WorkflowVisibility workflowsVisibleTo(
 *         final UserDetails currentUser) {
 *
 *     return WorkflowVisibility.workflowsAddressedTo(currentUser);
 *
 * }
 * </pre>
 */
public abstract class AbstractWorkflowListGuiApiController implements OfficialWorkflowlistApi {

    @Autowired
    protected UserContext userContext;

    @Autowired
    protected GuiApiMapper mapper;

    @Autowired
    protected io.vanillabp.cockpit.tasklist.api.v1.GuiApiMapper userTaskMapper;

    @Autowired
    protected WorkflowlistService workflowlistService;

    @Autowired
    protected UserTaskService userTaskService;

    /**
     * The workflows this view lets the given user reach, which is the only thing a subclass has to
     * decide. It is asked once per request and used for the list as well as for everything naming a
     * single workflow.
     */
    protected abstract WorkflowVisibility workflowsVisibleTo(
            final UserDetails currentUser);

    protected Page<Workflow> getWorkflows(
            final WorkflowVisibility visibility,
            final int pageNumber,
            final int pageSize,
            final OffsetDateTime initialTimestamp,
            final List<String> businessIds,
            final List<SearchQuery> searchQueries,
            final String sort,
            final boolean sortAscending,
            final WorkflowlistService.RetrieveItemsMode mode) {

        return workflowlistService.getWorkflows(
                pageNumber,
                pageSize,
                initialTimestamp,
                visibility,
                businessIds,
                searchQueries,
                sort,
                sortAscending,
                mode);

    }

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
                workflowsVisibleTo(currentUser),
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

    protected Page<Workflow> getWorkflowsUpdated(
            final WorkflowVisibility visibility,
            final int size,
            final Collection<String> knownWorkflowsIds,
            final OffsetDateTime initialTimestamp,
            final List<SearchQuery> searchQueries,
            final String sort,
            final boolean sortAscending,
            final WorkflowlistService.RetrieveItemsMode mode) {

        return workflowlistService.getWorkflowsUpdated(
                visibility,
                size,
                knownWorkflowsIds,
                initialTimestamp,
                searchQueries,
                sort,
                sortAscending,
                mode);

    }

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
                workflowsVisibleTo(currentUser),
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

    @Override
    public ResponseEntity<io.vanillabp.cockpit.gui.api.v1.Workflow> getWorkflow(
            final String workflowId) {

        final var currentUser = userContext.getUserLoggedInDetails();

        final var workflow = workflowlistService.getWorkflow(
                workflowsVisibleTo(currentUser), workflowId);

        return workflow == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(mapper.toApi(workflow));

    }

    /**
     * The user tasks belonging to one workflow, used by the status-site of a workflow. Which of them
     * are wanted depends on what the site shows, which is why the caller decides per request:
     *
     * @param limitListAccordingToCurrentUsersPermissions {@code true} lists only the tasks the
     *        current user could work on, the way the task list does; {@code false} lists every user
     *        task of the workflow, for a site showing what the workflow is up to rather than what
     *        the reader has to do
     */
    protected List<io.vanillabp.cockpit.tasklist.model.UserTask> getUserTasksOfWorkflow(
            final WorkflowVisibility visibility,
            final String workflowId,
            final boolean activeOnlyRequested,
            final boolean limitListAccordingToCurrentUsersPermissions,
            final UserDetails currentUser,
            final int pageSize,
            final String sort,
            final boolean sortAscending) {

        // the tasks belong to the detail view of the workflow, so a workflow the user may not see
        // has none to show, whichever of the two modes the caller asks for
        if (workflowlistService.getWorkflow(visibility, workflowId) == null) {
            return List.of();
        }

        return userTaskService.getUserTasksOfWorkflow(
                workflowId,
                activeOnlyRequested,
                limitListAccordingToCurrentUsersPermissions,
                currentUser.getId(),
                currentUser.getAuthorities(),
                pageSize,
                sort,
                sortAscending);

    }

    @Override
    public ResponseEntity<List<UserTask>> getUserTasksOfWorkflow(
            final String workflowId,
            final Boolean llatcup,
            final UserTasksRequest userTasksRequest) {

        final var currentUser = userContext.getUserLoggedInDetails();

        final var userTasks = getUserTasksOfWorkflow(
                workflowsVisibleTo(currentUser),
                workflowId,
                userTasksRequest.getMode() == UserTaskRetrieveMode.OPENTASKS,
                llatcup != null ? llatcup : true,
                currentUser,
                userTasksRequest.getPageSize() == null ? 100 : userTasksRequest.getPageSize(),
                userTasksRequest.getSort(),
                userTasksRequest.getSortAscending() == null || userTasksRequest.getSortAscending())
                .stream()
                .map(userTask -> userTaskMapper.toApi(userTask, currentUser.getId()))
                .toList();

        return ResponseEntity.ok(userTasks);

    }

    protected List<io.vanillabp.cockpit.util.kwic.KwicResult> kwic(
            final WorkflowVisibility visibility,
            final OffsetDateTime endedSince,
            final List<SearchQuery> searchQueries,
            final String path,
            final String query) {

        return workflowlistService.kwic(endedSince, visibility, searchQueries, path, query);

    }

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

        final var result = kwic(
                workflowsVisibleTo(currentUser), timestamp, searchQueries, effectivePath, query)
                .stream()
                .map(mapper::toApi)
                .toList();

        return ResponseEntity.ok(new KwicResults().result(result));

    }

}
