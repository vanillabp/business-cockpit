package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.bpms.api.v1.WorkflowCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.gui.api.v1.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Service class for managing Workflows.
 * Contains the Map {@code workflows} to store and retrieve tasks.
 */
@Service
public class WorkflowService {

    private final Logger log = LoggerFactory.getLogger(WorkflowService.class);

    private final Map<String, Workflow> workflows = new HashMap<>();

    /**
     * Creates a new UserTask from details and adds it to the workflow map.
     *
     * @param workflowId The ID of the UserTask to be created.
     */

    public void createWorkflow(
            final String workflowId,
            final WorkflowCreatedOrUpdatedEvent event) {

        if (workflowId == null) {
            throw new IllegalArgumentException("UserTask ID and details cannot be null");
        }

        final Workflow workflow = new Workflow();
        workflow.setId(workflowId);
        workflow.setBusinessId(event.getBusinessId());
        workflow.setWorkflowModuleId(event.getWorkflowModuleId());
        workflow.setTitle(event.getTitle() != null ? event.getTitle() : new HashMap<>());
        workflow.setComment(event.getComment());
        workflow.setBpmnProcessId(event.getBpmnProcessId());
        workflow.setBpmnProcessVersion(event.getBpmnProcessVersion());
        workflow.setDetails(event.getDetails() != null ? event.getDetails() : new HashMap<>());
        workflow.setDetailsFulltextSearch(event.getDetailsFulltextSearch());

        workflow.setId(workflowId);

        workflows.put(workflowId, workflow);
        log.info("Created workflow {}", workflowId);
    }

    /**
     * Updates an existing Workflow in the workflow map.
     *
     * @param workflowId The ID of the UserTask to update.
     * @param details The map containing task details.
     * @throws IllegalArgumentException If the task ID is null.
     * @throws IllegalStateException If the task does not exist.
     */
    public void updateWorkflow(
            final String workflowId,
            final Map<String, Object> details) {

        if (workflowId == null) {
            throw new IllegalArgumentException("Workflow ID cannot be null");
        }

        if (!workflows.containsKey(workflowId)) {
            throw new IllegalStateException("Workflow with ID " + workflowId + " not found");
        }

        workflows.get(workflowId).setDetails(details);
    }

    /**
     * gets specific workflow from workflow Map based on ID.
     *
     * @param workflowId unique ID for each workflow
     * @return returns the requested workflow object based on ID.
     */
    public Workflow getWorkflow(
            final String workflowId) {

        if (workflowId == null) {
            throw new IllegalArgumentException("Workflow ID cannot be null");
        }

        log.info("Getting workflow {}", workflowId);

        return workflows.get(workflowId);
    }

    /**
     * Removes specific workflow from {@code workflows}
     *
     * @param workflowId unique ID for each workflow
     */
    public void removeWorkflow(
            final String workflowId) {

        if (workflowId == null) {
            throw new IllegalArgumentException("Workflow ID cannot be null");
        }

        workflows.remove(workflowId);
        log.info("Workflow with ID {} removed", workflowId);
    }

}
