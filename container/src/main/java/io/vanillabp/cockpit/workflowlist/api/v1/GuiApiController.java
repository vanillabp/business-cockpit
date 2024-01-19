package io.vanillabp.cockpit.workflowlist.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.util.SearchQuery;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

@RestController("workflowListGuiApiController")
@RequestMapping(path = "/gui/api/v1")
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
            final List<SearchQuery> searchQueries,
            final String sort,
            final boolean sortAscending) {

        return workflowlistService
                .getWorkflows(
                        pageNumber,
                        pageSize,
                        initialTimestamp,
                        true,
                        null,
                        null,
                        searchQueries,
                        sort,
                        sortAscending);

    }

    @Override
    protected Mono<Page<Workflow>> getWorkflowsUpdated(
            final UserDetails currentUser,
            final int size,
            final Collection<String> knownWorkflowsIds,
            final OffsetDateTime initialTimestamp,
            final List<SearchQuery> searchQueries,
            final String sort,
            final boolean sortAscending) {

        return workflowlistService
                .getWorkflowsUpdated(
                        true,
                        null,
                        null,
                        size,
                        knownWorkflowsIds,
                        initialTimestamp,
                        searchQueries,
                        sort,
                        sortAscending);

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
            final boolean activeOnlyRequested) {

        return userTaskService
                .getUserTasksOfWorkflow(activeOnlyRequested, workflowId);

    }

    @Override
    protected Flux<WorkflowlistService.KwicResult> kwic(
            final UserDetails currentUser,
            final OffsetDateTime endedSince,
            final List<SearchQuery> searchQueries,
            final String path,
            final String query) {

        return workflowlistService
                .kwic(
                        endedSince,
                        true,
                        null,
                        null,
                        searchQueries,
                        path,
                        query);

    }

}
