package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Page;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowRepository;

/**
 * Keeps the workflows the simulator received. They are stored in the simulator's in-memory
 * database.
 */
@Service
@Transactional
public class WorkflowService {

  /**
   * Which workflows a query returns.
   */
  public enum RetrieveMode {
    /** All workflows, running and ended. */
    ALL,
    /** Only the workflows which have not ended yet. */
    ACTIVE,
    /** Only the workflows which have ended. */
    INACTIVE
  };

  @Autowired
  private WorkflowRepository workflows;

  /**
   * Creates the service. Spring creates it, nobody else does.
   */
  public WorkflowService() {
  }

  /**
   * Stores a new workflow.
   *
   * @param workflowId The ID of the Workflow to be created.
   * @param workflow The workflow to store
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
   * Updates a workflow which is stored already.
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
        .orElseThrow(() -> new IllegalStateException("Workflow with ID "
            + workflowId
            + " not found"));

    mapOnto.accept(workflowFound);

    workflows.save(workflowFound);


  }

  /**
   * Reads all workflows.
   *
   * @return List of all stored Workflow objects.
   */
  public List<Workflow> getAllWorkflows() {

    return workflows
        .findAll();

  }

  /**
   * Reads one workflow.
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
        .orElseThrow(() -> new IllegalStateException("Workflow with ID "
            + workflowId
            + " not found"));

  }

  /**
   * Reads one page of the workflows.
   *
   * <p>This method applies pagination to the full list of workflows. It also sets default values
   * if pagination parameters are not provided. The method returns only the current page of workflows
   * along with metadata like page size, number, and total pages.</p>
   *
   * @param retrieveMode Which workflows to return: all, only running ones or only ended ones
   * @param businessIds Only workflows with one of these business ids. All if {@code null} or empty.
   * @param sortField The field to sort by, {@code createdAt} if {@code null}
   * @param sortAscending {@code true} to sort ascending, otherwise descending
   * @param pageNumberRequested The number of the page, starting at 0. 0 if {@code null}.
   * @param pageSizeRequested The number of workflows of a page. 20 if {@code null}.
   * @return The page of workflows
   */
  public Page<Workflow> getWorkflows(
      final RetrieveMode retrieveMode,
      final List<String> businessIds,
      final String sortField,
      final Boolean sortAscending,
      final Integer pageNumberRequested,
      final Integer pageSizeRequested) {

    final String effectiveSortField = sortField != null ? sortField : "createdAt";
    final Sort.Direction direction = Boolean.TRUE.equals(sortAscending)
        ? Sort.Direction.ASC
        : Sort.Direction.DESC;
    final Sort sort = Sort.by(direction, effectiveSortField);

    List<Workflow> allWorkflows = switch (retrieveMode) {
      case ACTIVE -> workflows.findByEndedAtIsNull(sort);
      case INACTIVE -> workflows.findByEndedAtIsNotNull(sort);
      default -> workflows.findAll(sort);
    };

    if (businessIds != null && !businessIds.isEmpty()) {
      allWorkflows = allWorkflows
          .stream()
          .filter(w -> businessIds.contains(w.getBusinessId()))
          .collect(Collectors.toList());
    }

    // Set default pagination values if not provided
    int pageSize = pageSizeRequested != null ? pageSizeRequested : 20;
    int pageNumber = pageNumberRequested != null ? pageNumberRequested : 0;

    // Calculate total values
    long totalElements = allWorkflows.size();
    int totalPages = pageSize > 0 ? (int) Math.ceil((double) totalElements / pageSize) : 0;

    // Calculate page slice boundaries
    int fromIndex = (int) Math.min((long) pageNumber * pageSize, totalElements);
    int toIndex = (int) Math.min((long) fromIndex + pageSize, totalElements);
    List<Workflow> pagedWorkflows = allWorkflows.subList(fromIndex, toIndex);

    // Build the Page metadata
    return Page
        .<Workflow>builder()
        .number(pageNumber)
        .size(pageSize)
        .totalElements(totalElements)
        .totalPages(totalPages)
        .pageObjects(pagedWorkflows)
        .build();

  }

  /**
   * Counts the workflows a list of the given mode shows.
   *
   * @param retrieveMode Which workflows to count
   * @param businessIds Only workflows with one of these business ids. All if {@code null} or empty.
   * @return The number of workflows
   */
  public long countWorkflows(
      final RetrieveMode retrieveMode,
      final List<String> businessIds) {

    return getWorkflows(retrieveMode, businessIds, null, null, 0, Integer.MAX_VALUE)
        .getTotalElements();

  }

  /**
   * Marks a workflow as completed.
   *
   * @param workflowId The ID of the Workflow to complete.
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
        .orElseThrow(() -> new IllegalStateException("Workflow with ID "
            + workflowId
            + " not found"));

    mapOnto.accept(workflowFound);

    workflows.save(workflowFound);

  }

  /**
   * Marks a workflow as cancelled. An unknown workflow is ignored.
   *
   * @param workflowId The ID of the Workflow to cancel.
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
