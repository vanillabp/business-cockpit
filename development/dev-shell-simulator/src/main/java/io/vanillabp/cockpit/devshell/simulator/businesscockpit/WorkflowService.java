package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Page;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowRepository;

/**
 * Keeps the workflows the simulator received. They are stored in the simulator's in-memory
 * database.
 *
 * <p>Every method reads in a read-only transaction, and only the methods which change a workflow
 * open one which writes. A test of a workflow module asks again and again whether its workflow
 * has ended. If such a read wrote the rows it read, a read which started before the report of
 * the end and finished after it would store the old row again, and the end would be gone.
 *
 * <p>A method which changes a workflow runs through {@link ConflictingChanges}. Two reports about
 * the same workflow may arrive at the same time, and the second one is then laid onto what the
 * first one stored instead of undoing it.
 *
 * <p>Like the cockpit, the simulator takes the reports of a workflow in any order. A workflow
 * module sends them through an outbox, and an outbox may send a report again later, after the
 * reports which followed it. So a change or an end of a workflow the simulator does not hold yet
 * stores the workflow, and a creation of a workflow it holds already does not replace what the
 * later reports said.
 */
@Service
@Transactional(readOnly = true)
public class WorkflowService {

  /**
   * Which workflows a query returns.
   */
  public enum RetrieveMode {
    /** All workflows, running and ended. */
    ALL,
    /** Only the workflows which have not ended yet, suspended ones included. */
    INPROGRESS,
    /** Only the workflows which have ended. */
    DONE
  };

  @Autowired
  private WorkflowRepository workflows;

  @Autowired
  private ConflictingChanges changes;

  /**
   * Creates the service. Spring creates it, nobody else does.
   */
  public WorkflowService() {
  }

  /**
   * Stores a new workflow. If the simulator holds the workflow already, because a later report
   * arrived first or the creation is sent a second time, what is stored stays. The creation only
   * adds what the stored workflow does not say, and its start if that is earlier.
   *
   * @param workflowId The ID of the Workflow to be created.
   * @param workflow The workflow to store
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void createWorkflow(
      final String workflowId,
      final Workflow workflow) {

    if ((workflowId == null) || (workflow == null)) {
      throw new IllegalArgumentException("Workflow ID or workflow must not be null!");
    }

    changes.runAndRepeatOnConflict(() -> {
      final var stored = workflows.findById(workflowId);
      if (stored.isPresent()) {
        addWhatOnlyTheCreationSays(stored.get(), workflow);
        return workflows.saveAndFlush(stored.get());
      }
      // an attempt which failed on the key has given the object a version, and an object with a
      // version is taken for one which is stored already
      workflow.setVersion(null);
      return workflows.saveAndFlush(workflow);
    });

  }

  private static void addWhatOnlyTheCreationSays(
      final Workflow stored,
      final Workflow created) {

    if ((stored.getCreatedAt() == null) || ((created.getCreatedAt() != null) && created.getCreatedAt()
        .isBefore(stored.getCreatedAt()))) {
      stored.setCreatedAt(created.getCreatedAt());
    }
    if (stored.getUpdatedAt() == null) {
      stored.setUpdatedAt(created.getUpdatedAt());
    }
    if (stored.getInitiator() == null) {
      stored.setInitiator(created.getInitiator());
    }
    if (stored.getWorkflowModuleId() == null) {
      stored.setWorkflowModuleId(created.getWorkflowModuleId());
    }
    if (stored.getComment() == null) {
      stored.setComment(created.getComment());
    }
    if (stored.getBpmnProcessId() == null) {
      stored.setBpmnProcessId(created.getBpmnProcessId());
    }
    if (stored.getBpmnProcessVersion() == null) {
      stored.setBpmnProcessVersion(created.getBpmnProcessVersion());
    }
    if (stored.getBusinessId() == null) {
      stored.setBusinessId(created.getBusinessId());
    }
    if ((stored.getTitle() == null) || stored.getTitle().isEmpty()) {
      stored.setTitle(created.getTitle());
    }
    if (stored.getUiUri() == null) {
      stored.setUiUri(created.getUiUri());
    }
    if (stored.getUiUriType() == null) {
      stored.setUiUriType(created.getUiUriType());
    }
    if (stored.getWorkflowModuleUri() == null) {
      stored.setWorkflowModuleUri(created.getWorkflowModuleUri());
    }
    if (stored.getAccessibleToUsers() == null) {
      stored.setAccessibleToUsers(created.getAccessibleToUsers());
    }
    if (stored.getAccessibleToGroups() == null) {
      stored.setAccessibleToGroups(created.getAccessibleToGroups());
    }
    if (stored.getDetails() == null) {
      stored.setDetails(created.getDetails());
    }
    if (stored.getDetailsFulltextSearch() == null) {
      stored.setDetailsFulltextSearch(created.getDetailsFulltextSearch());
    }

  }

  /**
   * Lays a report onto the stored workflow, or onto a new one if the simulator does not hold it
   * yet, and stores the result.
   *
   * @param workflowId The id of the workflow
   * @param mapOnto Lays the report onto the workflow
   */
  private void layOntoTheWorkflow(
      final String workflowId,
      final Consumer<Workflow> mapOnto) {

    changes.runAndRepeatOnConflict(() -> {
      final var workflow = workflows
          .findById(workflowId)
          .orElseGet(() -> {
            final var notReportedYet = new Workflow();
            notReportedYet.setId(workflowId);
            return notReportedYet;
          });
      mapOnto.accept(workflow);
      if (workflow.getCreatedAt() == null) {
        // the creation has not arrived yet. Until it does, the earliest moment the simulator
        // knows of is the time of this report
        workflow.setCreatedAt(workflow.getUpdatedAt());
      }
      return workflows.saveAndFlush(workflow);
    });

  }

  /**
   * Changes a workflow. A workflow the simulator does not hold yet is stored as the report says.
   *
   * @param workflowId The ID of the Workflow to update.
   * @param mapOnto    Mapper
   * @throws IllegalArgumentException If the workflow ID is null.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void updateWorkflow(
      final String workflowId,
      final Consumer<Workflow> mapOnto) {

    if (workflowId == null) {
      throw new IllegalArgumentException("Workflow ID must not be null!");
    }

    layOntoTheWorkflow(workflowId, mapOnto);

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
   * @param workflowId The id of the workflow
   * @return The workflow, or nothing if the simulator holds none of that id
   */
  public Optional<Workflow> getWorkflow(
      final String workflowId) {

    if (workflowId == null) {
      throw new IllegalArgumentException("Workflow ID must not be null");
    }

    return workflows.findById(workflowId);

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
      case INPROGRESS -> workflows.findByEndedAtIsNull(sort);
      case DONE -> workflows.findByEndedAtIsNotNull(sort);
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
   * Marks a workflow as completed. A workflow the simulator does not hold yet is stored as
   * completed, so it does not show as running once its creation arrives.
   *
   * @param workflowId The ID of the Workflow to complete.
   * @param mapOnto    Mapper
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void completeWorkflow(
      final String workflowId,
      final Consumer<Workflow> mapOnto) {

    if (workflowId == null) {
      throw new IllegalArgumentException("Workflow ID must not be null!");
    }

    layOntoTheWorkflow(workflowId, mapOnto);

  }

  /**
   * Marks a workflow as cancelled. A workflow the simulator does not hold yet is stored as
   * cancelled, for the same reason as a completed one.
   *
   * @param workflowId The ID of the Workflow to cancel.
   * @param mapOnto    Mapper
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void cancelWorkflow(
      final String workflowId,
      final Consumer<Workflow> mapOnto) {

    if (workflowId == null) {
      throw new IllegalArgumentException("Workflow ID must not be null!");
    }

    layOntoTheWorkflow(workflowId, mapOnto);

  }

  /**
   * Sets the note of one entry, or clears it where the text is empty. The simulator follows the
   * cockpit: an ended entry keeps its note.
   *
   * @param workflowId The id
   * @param text The text of the note, or {@code null}
   * @return The entry as it is stored now, or {@code null} if there is none of that id
   * @throws IllegalStateException if the entry has ended
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public Workflow setNote(
      final String workflowId,
      final String text) {

    return changes.runAndRepeatOnConflict(() -> {
      final var found = workflows.findById(workflowId).orElse(null);
      if (found == null) {
        return null;
      }
      if (found.getEndedAt() != null) {
        throw new IllegalStateException("'"
            + workflowId
            + "' has ended");
      }
      found.setNote(StringUtils.hasText(text) ? text : null);
      return workflows.saveAndFlush(found);
    });

  }

  /**
   * Marks a workflow as suspended, or removes the mark. A workflow the simulator does not hold stays
   * unknown, like in the cockpit. An ended one takes the report as well, because only the reports
   * of a suspension and an activation change the mark.
   *
   * @param workflowId The id
   * @param suspendedAt When the process instance was suspended, or {@code null} where it was
   *        activated again
   * @param suspendedBy Who suspended or activated it, or {@code null} where nobody is named
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void changeSuspension(
      final String workflowId,
      final OffsetDateTime suspendedAt,
      final String suspendedBy) {

    changes.runAndRepeatOnConflict(() -> {
      final var found = workflows.findById(workflowId);
      found.ifPresent(workflow -> {
        workflow.setSuspendedAt(suspendedAt);
        workflow.setSuspendedBy(suspendedBy);
        workflows.saveAndFlush(workflow);
      });
      return found;
    });

  }

}
