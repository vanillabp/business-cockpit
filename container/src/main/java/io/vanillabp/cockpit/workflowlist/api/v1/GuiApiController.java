package io.vanillabp.cockpit.workflowlist.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.util.SearchQuery;
import io.vanillabp.cockpit.util.kwic.KwicResult;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A workflowlist API controller which gives access to the workflows
 * <ul>
 *     <li>which name the user in their accessibleToUsers</li>
 *     <li>which name one of the user's groups in their accessibleToGroups</li>
 *     <li>which name neither users nor groups (dangling workflows, open to everyone)</li>
 * </ul>
 *
 * <p>The user's groups are the authorities of the current request, which the JWT filter has already
 * widened by everything the registered workflow modules grant through their group hierarchy. The
 * task list of this application reads them from the same place.
 *
 * <p>The detail view answers by the same rule, so a workflow kept out of the list cannot be opened
 * by guessing its id either. The same holds for the user tasks of a workflow, which would otherwise
 * tell a user about a workflow they may not open.
 */
@RestController("workflowListGuiApiController")
@RequestMapping(path = "/gui/api/v1")
public class GuiApiController extends AbstractWorkflowListGuiApiController {

    @Autowired
    private WorkflowlistService workflowlistService;

    @Autowired
    private UserTaskService userTaskService;

    /**
     * Named in a workflow's accessibleToUsers a user is addressed individually, next to the groups
     * they belong to.
     */
    private static List<String> accessibleToUsers(
            final UserDetails currentUser) {

        return List.of(currentUser.getId());

    }

    @Override
    protected Page<Workflow> getWorkflows(
            final UserDetails currentUser,
            final int pageNumber,
            final int pageSize,
            final OffsetDateTime initialTimestamp,
            final List<String> businessIds,
            final List<SearchQuery> searchQueries,
            final String sort,
            final boolean sortAscending,
            final WorkflowlistService.RetrieveItemsMode mode) {

        return workflowlistService
                .getWorkflows(
                        pageNumber,
                        pageSize,
                        initialTimestamp,
                        true,
                        accessibleToUsers(currentUser),
                        currentUser.getAuthorities(),
                        businessIds,
                        searchQueries,
                        sort,
                        sortAscending,
                        mode);

    }

    @Override
    protected Page<Workflow> getWorkflowsUpdated(
            final UserDetails currentUser,
            final int size,
            final Collection<String> knownWorkflowsIds,
            final OffsetDateTime initialTimestamp,
            final List<SearchQuery> searchQueries,
            final String sort,
            final boolean sortAscending,
            final WorkflowlistService.RetrieveItemsMode mode) {

        return workflowlistService
                .getWorkflowsUpdated(
                        true,
                        accessibleToUsers(currentUser),
                        currentUser.getAuthorities(),
                        size,
                        knownWorkflowsIds,
                        initialTimestamp,
                        searchQueries,
                        sort,
                        sortAscending,
                        mode);

    }

    @Override
    protected Workflow getWorkflow(
            final UserDetails currentUser,
            final String workflowId) {

        final var workflow = workflowlistService.getWorkflow(workflowId);

        // a workflow the user may not see is reported as unknown, which keeps the detail view from
        // confirming that it exists
        return isAccessibleTo(workflow, currentUser.getId(), currentUser.getAuthorities())
                ? workflow
                : null;

    }

    /**
     * The rule the list query applies, spelled out for a single workflow: a workflow carrying no
     * access information at all is open to everyone, any other one is shown to the users it names
     * and to the members of the groups it names.
     */
    private boolean isAccessibleTo(
            final Workflow workflow,
            final String currentUserId,
            final Collection<String> currentUsersGroups) {

        if (workflow == null) {
            return false;
        }
        if (workflow.isDangling()) {
            return true;
        }

        final var addressesTheUser = Optional
                .ofNullable(workflow.getAccessibleToUsers())
                .orElseGet(List::of)
                .stream()
                .anyMatch(person -> currentUserId.equals(person.getId()));
        if (addressesTheUser) {
            return true;
        }

        return Optional
                .ofNullable(workflow.getAccessibleToGroups())
                .orElseGet(List::of)
                .stream()
                .map(Group::getId)
                .anyMatch(currentUsersGroups::contains);

    }

    @Override
    protected List<UserTask> getUserTasksOfWorkflow(
            final String workflowId,
            final boolean activeOnlyRequested,
            final boolean limitListAccordingToCurrentUsersPermissions,
            final String currentUser,
            final Collection<String> currentUserGroups,
            final int pageSize,
            final String sort,
            final boolean sortAscending) {

        // the user tasks belong to the detail view of the workflow, so a workflow the user may not
        // see has none to show, whichever of the two modes the caller asks for
        if (!isAccessibleTo(
                workflowlistService.getWorkflow(workflowId), currentUser, currentUserGroups)) {
            return List.of();
        }

        return userTaskService
                .getUserTasksOfWorkflow(
                        workflowId,
                        activeOnlyRequested,
                        limitListAccordingToCurrentUsersPermissions,
                        currentUser,
                        currentUserGroups,
                        pageSize,
                        sort,
                        sortAscending);

    }

    @Override
    protected List<KwicResult> kwic(
            final UserDetails currentUser,
            final OffsetDateTime endedSince,
            final List<SearchQuery> searchQueries,
            final String path,
            final String query) {

        return workflowlistService
                .kwic(
                        endedSince,
                        true,
                        accessibleToUsers(currentUser),
                        currentUser.getAuthorities(),
                        searchQueries,
                        path,
                        query);

    }

}
