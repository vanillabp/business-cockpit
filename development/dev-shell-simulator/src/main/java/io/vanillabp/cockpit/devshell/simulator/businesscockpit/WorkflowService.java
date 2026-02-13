package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Page;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowRepository;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for managing Workflows.
 * Contains the Map {@code workflows} to store and retrieve tasks.
 */
@Service
@Transactional
public class WorkflowService {

    public enum RetrieveMode {
        ALL,
        ACTIVE,
        INACTIVE
    };

    @Autowired
    private WorkflowRepository workflows;

    /**
     * Creates a new Workflow from details and adds it to the workflow map.
     *
     * @param workflowId The ID of the Workflow to be created.
     */
    public void createWorkflow(
            final String workflowId,
            final Workflow workflow) {

        if ((workflowId == null) || (workflow == null)) {
            throw new IllegalArgumentException("Workflow ID or workflow must not be null!");
        }

        workflows.save(workflow);

    }

    /**
     * Updates an existing Workflow in the workflow map.
     *
     * @param workflowId The ID of the Workflow to update.
     * @param mapOnto    Mapper
     * @throws IllegalArgumentException If the workflow ID is null.
     * @throws IllegalStateException    If the workflow does not exist.
     */
    public void updateWorkflow(
            final String workflowId,
            final Consumer<Workflow> mapOnto) {

        if (workflowId == null) {
            throw new IllegalArgumentException("Workflow ID must not be null!");
        }

        final var workflowFound = workflows
                .findById(workflowId)
                .orElseThrow(() -> new IllegalStateException("Workflow with ID " + workflowId + " not found"));

        mapOnto.accept(workflowFound);

        workflows.save(workflowFound);


    }

    /**
     * Retrieves all workflows from the HashMap.
     *
     * @return List of all stored Workflow objects.
     */
    public List<Workflow> getAllWorkflows() {

        return workflows
                .findAll();

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

        return workflows
                .findById(workflowId)
                .orElseThrow(() -> new IllegalStateException("Workflow with ID " + workflowId + " not found"));

    }

    /**
     * Retrieves a paginated list of workflows based on the provided request parameters.
     *
     * <p>This method applies pagination to the full list of workflows. It also sets default values
     * if pagination parameters are not provided. The method returns only the current page of workflows
     * along with metadata like page size, number, and total pages.</p>
     */
    public Page<Workflow> getWorkflows(
            final RetrieveMode retrieveMode,
            final Integer pageNumberRequested,
            final Integer pageSizeRequested) {

        List<Workflow> allWorkflows = switch(retrieveMode) {
            case ACTIVE -> workflows.findByEndedAtIsNull();
            case INACTIVE -> workflows.findByEndedAtIsNotNull();
            default -> getAllWorkflows();
        };

        // Set default pagination values if not provided
        int pageSize = pageSizeRequested != null ? pageSizeRequested : 20;
        int pageNumber = pageNumberRequested != null ? pageNumberRequested : 0;

        // Calculate total values
        int totalElements = allWorkflows.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);

        // Calculate page slice boundaries
        int fromIndex = Math.min(pageNumber * pageSize, totalElements);
        int toIndex = Math.min(fromIndex + pageSize, totalElements);
        List<Workflow> pagedWorkflows = allWorkflows.subList(fromIndex, toIndex);

        // Build the Page metadata
        return Page
                .<Workflow>builder()
                .number(pageNumber)
                .size(pageSize)
                .totalPages(totalPages)
                .pageObjects(pagedWorkflows)
                .build();

    }

    /**
     * Updates an existing Workflow in the workflow map.
     *
     * @param workflowId The ID of the Workflow to update.
     * @param mapOnto    Mapper
     */
    public void completeWorkflow(
            final String workflowId,
            final Consumer<Workflow> mapOnto) {

        if (workflowId == null) {
            throw new IllegalArgumentException("Workflow ID must not be null!");
        }

        final var workflowFound = workflows
                .findById(workflowId)
                .orElseThrow(() -> new IllegalStateException("Workflow with ID " + workflowId + " not found"));

        mapOnto.accept(workflowFound);

        workflows.save(workflowFound);

    }

    /** Updates an existing Workflow in the workflow map.
     *
     * @param workflowId The ID of the Workflow to update.
     * @param mapOnto    Mapper
     */
    public void cancelWorkflow(
            final String workflowId,
            final Consumer<Workflow> mapOnto) {

        if (workflowId == null) {
            throw new IllegalArgumentException("Workflow ID must not be null!");
        }

        workflows
                .findById(workflowId)
                .ifPresent(workflowFound -> {
                    mapOnto.accept(workflowFound);
                    workflows.save(workflowFound);
                });

    }

}
