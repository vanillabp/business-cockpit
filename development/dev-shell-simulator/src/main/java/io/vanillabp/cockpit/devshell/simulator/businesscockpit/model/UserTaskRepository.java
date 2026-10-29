package io.vanillabp.cockpit.devshell.simulator.businesscockpit.model;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Reads and writes the stored user tasks. Which of them a list shows is decided in memory, by
 * {@link io.vanillabp.cockpit.devshell.simulator.businesscockpit.TaskService.RetrieveMode}.
 */
@Repository
public interface UserTaskRepository extends JpaRepository<UserTask, String> {

  /**
   * The user tasks of one workflow.
   *
   * @param workflowId The id of the workflow
   * @param sort The order of the result
   * @return The user tasks
   */
  List<UserTask> findByWorkflowId(
      String workflowId,
      Sort sort);

}
