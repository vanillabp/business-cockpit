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

/**
 * Turns the workflowlist requests of the GUI API into calls of {@link WorkflowlistService} and maps
 * the result back, but leaves open which workflows the current user gets to see. That decision
 * belongs to the application built on this library, because only it knows what its workflows are
 * about: a cockpit for a support team may want every workflow in one list, another one may show
 * nobody anything the reporting workflow module did not explicitly address to them.
 *
 * <p>The subclass answers by passing a filter to the service. Every retrieval method takes an
 * {@code accessibleToUsers} and an {@code accessibleToGroups} collection, and a workflow is included
 * once it names one of the given users or one of the given groups. Passing {@code null} for both
 * drops the restriction and lists every workflow. The {@code includeDanglingWorkflows} flag decides
 * what happens to a workflow which names neither users nor groups: passing {@code true} treats it as
 * open to everyone, {@code false} hides it from everyone.
 *
 * <p>The reference application filters by the current user and the authorities of the request, which
 * are the user's groups after the group hierarchies of all registered workflow modules have been
 * applied:
 *
 * <pre>
 * &#64;Override
 * protected Page&lt;Workflow&gt; getWorkflows(
 *         final UserDetails currentUser, ...) {
 *
 *     return workflowlistService.getWorkflows(
 *             pageNumber, pageSize, initialTimestamp,
 *             true,
 *             List.of(currentUser.getId()),
 *             currentUser.getAuthorities(),
 *             businessIds, searchQueries, sort, sortAscending, mode);
 *
 * }
 * </pre>
 *
 * <p>A list filter alone does not protect a workflow. {@link #getWorkflow(UserDetails, String)}
 * fetches one by its id, and the user tasks of a workflow are fetched by that id as well, so a
 * subclass which hides workflows from the list wants both of them to answer nothing for the same
 * workflows. Otherwise the detail view hands out what the list withheld.
 */
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

    /**
     * @return the workflow, or {@code null} if it does not exist or the current user may not see it
     *         - both are answered as HTTP 404, so the detail view does not tell one from the other
     */
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

    /**
     * The user tasks belonging to one workflow, used by the status-site of a workflow. Which of them
     * are wanted depends on what the site shows, which is why the caller decides per request:
     *
     * @param limitListAccordingToCurrentUsersPermissions {@code true} lists only the tasks the
     *        current user could work on, the way the task list does; {@code false} lists every user
     *        task of the workflow, for a site showing what the workflow is up to rather than what
     *        the reader has to do
     */
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
