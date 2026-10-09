package io.vanillabp.cockpit.notification;

import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * The BPMN process of the business case a user task belongs to. A user sets notifications per
 * such process, so it is what groups the user tasks on the page for notification settings and
 * what a setting is looked up by.
 * <p>
 * A user task names the process it sits in, and that is not always the process of its case. A
 * workflow may split its work into processes of their own and start them by call activities.
 * To the user that is still one process, only split up to keep the models readable. So a task of
 * such a called process belongs to the group of the case, and a setting of the case applies to
 * it. A called process with a workflow aggregate of its own is a case of its own and keeps its
 * own group. See decision 61 in the repository's DECISIONS.md.
 * <p>
 * The cockpit learns which case a task belongs to from the task's <code>workflowId</code>. That
 * is the case the workflow module files the task under, so the case's process is the process of
 * the workflow stored under that id. Where the cockpit holds no such workflow, the task is its
 * own case. That happens where the report of the workflow has not arrived yet, and for a
 * workflow module which reports no workflows.
 * <p>
 * {@code UserTaskService#getVisibleWorkflows} answers the same question inside a query of
 * MongoDB, by the same rule.
 *
 * @param workflowModuleId The workflow module of the case
 * @param bpmnProcessId The BPMN process of the case
 */
public record CaseProcess(
        String workflowModuleId,
        String bpmnProcessId) {

    /**
     * The case of a task the cockpit holds no workflow for: the process the task sits in.
     *
     * @param task The user task
     * @return Its own workflow module and BPMN process
     */
    public static CaseProcess ofTheTaskItself(
            final UserTask task) {

        return new CaseProcess(task.getWorkflowModuleId(), task.getBpmnProcessId());

    }

    /**
     * Reads the cases of several user tasks with one query.
     *
     * @param mongoTemplate Where the workflows are stored
     * @param tasks The user tasks
     * @return The case process of each of these tasks
     */
    public static Function<UserTask, CaseProcess> of(
            final MongoTemplate mongoTemplate,
            final Collection<UserTask> tasks) {

        final var workflowIds = tasks
                .stream()
                .map(UserTask::getWorkflowId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (workflowIds.isEmpty()) {
            return CaseProcess::ofTheTaskItself;
        }
        final var query = Query.query(Criteria.where("_id").in(workflowIds));
        query.fields().include("workflowModuleId", "bpmnProcessId");
        final var found = mongoTemplate.find(query, Workflow.class);
        final Map<String, CaseProcess> byWorkflowId = found == null
                ? Map.of()
                : found
                        .stream()
                        .filter(workflow -> workflow.getBpmnProcessId() != null)
                        .collect(Collectors.toMap(
                                Workflow::getId,
                                workflow -> new CaseProcess(
                                        workflow.getWorkflowModuleId(), workflow.getBpmnProcessId()),
                                (first, second) -> first));
        return task -> task.getWorkflowId() == null
                ? ofTheTaskItself(task)
                : byWorkflowId.getOrDefault(task.getWorkflowId(), ofTheTaskItself(task));

    }

}
