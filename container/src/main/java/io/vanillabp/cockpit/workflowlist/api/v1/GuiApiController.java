package io.vanillabp.cockpit.workflowlist.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.util.SearchQuery;
import io.vanillabp.cockpit.util.kwic.KwicResult;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController("workflowListGuiApiController")
@RequestMapping(path = {
        "/gui/api/v1",
        "/gui/api/v1/mine"
})
public class GuiApiController extends AbstractWorkflowListGuiApiController {

    @Autowired
    private WorkflowlistService workflowlistService;

    @Autowired
    private UserTaskService userTaskService;

    @Override
    protected Mono<Page<Workflow>> getWorkflows(
            final UserDetails currentUser,
            final int pageNumber,
            final int pageSize,
            final OffsetDateTime initialTimestamp,
            final List<String> businessIds,
            final List<SearchQuery> searchQueries,
            final String sort,
            final boolean sortAscending,
            final WorkflowlistService.RetrieveItemsMode mode,
            final List<String> workflowModuleIds) {

        return workflowlistService
                .getWorkflows(
                        pageNumber % 100,
                        pageSize,
                        initialTimestamp,
                        true,
                        List.of(currentUser.getId()),
                        currentUser.getAuthorities(),
                        businessIds,
                        searchQueries,
                        sort,
                        sortAscending,
                        mode,
                        workflowModuleIds)
                .map(result -> new PageImpl<>(
                      result.getContent(),
                      PageRequest.of(pageNumber, result.getPageable().getPageSize(), result.getPageable().getSort()),
                      1000000
                ));

    }

    @Override
    protected Mono<Page<Workflow>> getWorkflowsUpdated(
            final UserDetails currentUser,
            final int size,
            final Collection<String> knownWorkflowsIds,
            final OffsetDateTime initialTimestamp,
            final List<SearchQuery> searchQueries,
            final String sort,
            final boolean sortAscending,
            final WorkflowlistService.RetrieveItemsMode mode,
            final List<String> workflowModuleIds) {

        return workflowlistService
                .getWorkflowsUpdated(
                        true,
                        List.of(currentUser.getId()),
                        currentUser.getAuthorities(),
                        size,
                        knownWorkflowsIds,
                        initialTimestamp,
                        searchQueries,
                        sort,
                        sortAscending,
                        mode,
                        workflowModuleIds);

    }

    @Override
    protected Mono<Workflow> getWorkflow(
            final UserDetails currentUser,
            final String workflowId) {

        return workflowlistService
                .getWorkflow(workflowId);

    }

    @Override
    protected Flux<UserTask> getUserTasksOfWorkflow(
            final String workflowId,
            final boolean activeOnlyRequested,
            final boolean limitListAccordingToCurrentUsersPermissions,
            final String currentUser,
            final Collection<String> currentUserGroups,
            final int pageSize,
            final String sort,
            final boolean sortAscending) {

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
    protected Flux<KwicResult> kwic(
            final UserDetails currentUser,
            final OffsetDateTime endedSince,
            final List<SearchQuery> searchQueries,
            final String path,
            final String query,
            final List<String> workflowModuleIds) {

        return workflowlistService
                .kwic(
                        endedSince,
                        true,
                        null,
                        null,
                        searchQueries,
                        path,
                        query,
                        workflowModuleIds);

    }

}
