package io.vanillabp.cockpit.workflowlist.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.UserContext;
import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.gui.api.v1.KwicRequest;
import io.vanillabp.cockpit.gui.api.v1.KwicResults;
import io.vanillabp.cockpit.gui.api.v1.OfficialWorkflowlistApi;
import io.vanillabp.cockpit.gui.api.v1.UpdateStreams;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.gui.api.v1.UserTaskRetrieveMode;
import io.vanillabp.cockpit.gui.api.v1.UserTasksRequest;
import io.vanillabp.cockpit.gui.api.v1.Workflows;
import io.vanillabp.cockpit.gui.api.v1.WorkflowsRequest;
import io.vanillabp.cockpit.gui.api.v1.WorkflowsUpdateRequest;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.util.ListPaging;
import io.vanillabp.cockpit.util.SearchQuery;
import io.vanillabp.cockpit.workflowlist.WorkflowVisibility;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowlist.api.WorkflowStreamAudience;
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
 * Turns the workflowlist requests of the GUI API into calls of {@link WorkflowlistService} and
 * maps the result back. One question stays open, and the application built on this library answers
 * it: which workflows this view lets the person making the request reach. Only that application
 * knows what its workflows are about. A cockpit for a support team may want every workflow in one
 * list. Another one may show nobody anything the reporting workflow module did not address to
 * them.
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

    /** Tells who is logged in. */
    @Autowired
    protected UserContext userContext;

    /** Turns stored workflows into GUI API answers, and request values into model values. */
    @Autowired
    protected GuiApiMapper mapper;

    /** Turns the stored user tasks of a workflow into what the GUI API answers. */
    @Autowired
    protected io.vanillabp.cockpit.tasklist.api.v1.GuiApiMapper userTaskMapper;

    /** Reads the stored workflows. */
    @Autowired
    protected WorkflowlistService workflowlistService;

    /** Reads the user tasks of a workflow. */
    @Autowired
    protected UserTaskService userTaskService;

    /** Learns which workflows a browser shows, so it can tell the browser about their changes. */
    @Autowired
    protected UpdateStreams updateStreams;

    /**
     * Used by the subclasses.
     */
    public AbstractWorkflowListGuiApiController() {

    }

    /**
     * The workflows this view lets the given user reach, which is the only thing a subclass has to
     * decide. It is asked once per request and used for the list as well as for everything naming a
     * single workflow.
     *
     * @param currentUser The user who makes the request
     * @return The workflows the user may reach in this view
     */
    protected abstract WorkflowVisibility workflowsVisibleTo(
            final UserDetails currentUser);

    /**
     * One page of the list. A subclass may override it to read the workflows in another way.
     *
     * @param visibility Which workflows the user may reach
     * @param pageNumber The number of the page, starting at 0
     * @param pageSize How many workflows a page holds
     * @param initialTimestamp When the user started to page through the list
     * @param businessIds Only the workflows of these business ids, or {@code null} for all
     * @param searchQueries The filters of the list, or {@code null}
     * @param sort The paths to sort by, separated by commas, or {@code null}
     * @param sortAscending Whether to sort in ascending order
     * @param mode Which workflows the list holds
     * @return The page
     */
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

        final var visibility = workflowsVisibleTo(currentUser);
        final var workflows = getWorkflows(
                visibility,
                ListPaging.pageNumber(workflowsRequest.getPageNumber()),
                ListPaging.pageSize(workflowsRequest.getPageSize()),
                timestamp,
                workflowsRequest.getBusinessIds(),
                mapper.toModel(workflowsRequest.getSearchQueries()),
                workflowsRequest.getSort(),
                ListPaging.sortAscending(workflowsRequest.getSortAscending()),
                workflowsRequest.getMode() != null
                        ? mapper.toModel(workflowsRequest.getMode())
                        : WorkflowlistService.RetrieveItemsMode.All);

        tellTheUpdateStreamsWhatIsShown(currentUser, visibility, workflows);

        return ResponseEntity.ok(mapper.toApi(workflows, timestamp, requestId));

    }

    /**
     * The first workflows of the list again, for a browser which shows some of them already. A
     * subclass may override it to read the workflows in another way.
     *
     * @param visibility Which workflows the user may reach
     * @param size How many workflows to return at most
     * @param knownWorkflowsIds The ids of the workflows the browser shows already
     * @param initialTimestamp When the user started to page through the list
     * @param searchQueries The filters of the list, or {@code null}
     * @param sort The paths to sort by, separated by commas, or {@code null}
     * @param sortAscending Whether to sort in ascending order
     * @param mode Which workflows the list holds
     * @return The workflows as one page
     */
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

        final var visibility = workflowsVisibleTo(currentUser);
        final var workflows = getWorkflowsUpdated(
                visibility,
                workflowsUpdateRequest.getSize(),
                workflowsUpdateRequest.getKnownWorkflowsIds(),
                timestamp,
                mapper.toModel(workflowsUpdateRequest.getSearchQueries()),
                workflowsUpdateRequest.getSort(),
                ListPaging.sortAscending(workflowsUpdateRequest.getSortAscending()),
                workflowsUpdateRequest.getMode() != null
                        ? mapper.toModel(workflowsUpdateRequest.getMode())
                        : WorkflowlistService.RetrieveItemsMode.Active);

        tellTheUpdateStreamsWhatIsShown(currentUser, visibility, workflows);

        return ResponseEntity.ok(mapper.toApi(workflows, timestamp, requestId));

    }

    /**
     * The update streams of this person remember which workflows their browser shows now. That is
     * how a stream learns to wake up a list for a workflow the person may no longer see, see
     * {@link UpdateStreams}.
     */
    private void tellTheUpdateStreamsWhatIsShown(
            final UserDetails currentUser,
            final WorkflowVisibility visibility,
            final Page<Workflow> workflows) {

        updateStreams.listAnswered(
                WorkflowStreamAudience.KIND_OF_ENTITY,
                currentUser,
                visibility,
                workflows
                        .getContent()
                        .stream()
                        .map(Workflow::getId)
                        .toList());

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
     * @param visibility Which workflows the user may reach. A workflow outside of it has no tasks
     *        to show.
     * @param workflowId The id of the workflow
     * @param activeOnlyRequested Whether to leave out the tasks which have ended
     * @param limitListAccordingToCurrentUsersPermissions {@code true} lists only the tasks the
     *        current user could work on, the way the task list does. {@code false} lists every user
     *        task of the workflow, for a site which shows what the workflow is up to and not what
     *        the reader has to do
     * @param currentUser The user who makes the request
     * @param pageSize How many tasks to return at most
     * @param sort The paths to sort by, separated by commas, or {@code null}
     * @param sortAscending Whether to sort in ascending order
     * @return The tasks, or an empty list if the user may not see the workflow
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
                ListPaging.sortAscending(userTasksRequest.getSortAscending()))
                .stream()
                .map(userTask -> userTaskMapper.toApi(userTask, currentUser.getId()))
                .toList();

        return ResponseEntity.ok(userTasks);

    }

    /**
     * Suggestions for a search field of the list. A subclass may override it to search in another
     * way.
     *
     * @param visibility Which workflows the user may reach
     * @param endedSince When the user started to page through the list
     * @param searchQueries The filters the user set in the list
     * @param path The field to look for words in
     * @param query The text typed so far
     * @return The words found
     */
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
