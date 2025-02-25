package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.bpms.api.v1.WorkflowCancelledEvent;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCompletedEvent;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.gui.api.v1.Workflow;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service class for managing Workflows.
 * Contains the Map {@code workflows} to store and retrieve tasks.
 */
@Service
public class WorkflowService {

    private final Map<String, Workflow> workflows = new HashMap<>();

    @Autowired
    private OfficialApiMapper mapper;

    /**
     * Creates a new Workflow from details and adds it to the workflow map.
     *
     * @param workflowId The ID of the Workflow to be created.
     */
    public void createWorkflow(
            final String workflowId,
            final WorkflowCreatedOrUpdatedEvent event) {

        if ((workflowId == null) || (event == null)) {
            throw new IllegalArgumentException("Workflow ID or event must not be null!");
        }

        final var workflow = mapper.toApi(event);
        workflows.put(workflowId, workflow);

    }

    /**
     * Updates an existing Workflow in the workflow map.
     *
     * @param workflowId The ID of the Workflow to update.
     * @param event      WorkflowCreatedOrUpdatedEvent containing update details.
     * @throws IllegalArgumentException If the workflow ID is null.
     * @throws IllegalStateException    If the workflow does not exist.
     */
    public void updateWorkflow(
            final String workflowId,
            final WorkflowCreatedOrUpdatedEvent event) {

        if ((workflowId == null) || (event == null)) {
            throw new IllegalArgumentException("Workflow ID or event must not be null!");
        }
        if (!workflows.containsKey(workflowId)) {
            throw new IllegalStateException("Workflow with ID " + workflowId + " not found");
        }

        final var workflow = workflows.get(workflowId);
        mapper.ontoApi(workflow, event);

    }

    /**
     * Retrieves all workflows from the HashMap.
     *
     * @return List of all stored Workflow objects.
     */
    public List<Workflow> getAllWorkflows() {

        return workflows
                .values()
                .stream()
                .toList();

    }

    /**
     * Gets a specific workflow from the workflow map based on ID.
     *
     * @param workflowId Unique ID for each workflow.
     * @return Returns the requested workflow object based on ID.
     */
    public Workflow getWorkflow(
            final String workflowId) {

        if (workflowId == null) {
            throw new IllegalArgumentException("Workflow ID must not be null");
        }

        return workflows.get(workflowId);

    }

    /**
     * Updates an existing Workflow in the workflow map.
     *
     * @param workflowId The ID of the Workflow to update.
     * @param event      WorkflowCompletedEvent containing update details.
     */
    public void completeWorkflow(
            final String workflowId,
            final WorkflowCompletedEvent event) {

        if ((workflowId == null) || (event == null)) {
            throw new IllegalArgumentException("Workflow ID or event must not be null!");
        }

        final var workflow = workflows.get(workflowId);
        mapper.ontoApi(workflow, event);

    }

    /** Updates an existing Workflow in the workflow map.
     *
     * @param workflowId The ID of the Workflow to update.
     * @param event      WorkflowCancelledEvent containing update details.
     */
    public void cancelWorkflow(
            final String workflowId,
            final WorkflowCancelledEvent event) {

        if ((workflowId == null) || (event == null)) {
            throw new IllegalArgumentException("Workflow ID or event must not be null!");
        }

        final var workflow = workflows.get(workflowId);
        mapper.ontoApi(workflow, event);

    }

}
