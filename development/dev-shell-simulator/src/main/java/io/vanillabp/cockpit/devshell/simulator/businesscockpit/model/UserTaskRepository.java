package io.vanillabp.cockpit.devshell.simulator.businesscockpit.model;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Reads and writes the stored user tasks.
 */
@Repository
public interface UserTaskRepository extends JpaRepository<UserTask, String> {

  /**
   * The user tasks which have not ended yet.
   *
   * @param sort The order of the result
   * @return The user tasks
   */
  List<UserTask> findByEndedAtIsNull(
      Sort sort);

  /**
   * The user tasks which have ended.
   *
   * @param sort The order of the result
   * @return The user tasks
   */
  List<UserTask> findByEndedAtIsNotNull(
      Sort sort);

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

  /**
   * The user tasks of one workflow which have not ended yet.
   *
   * @param workflowId The id of the workflow
   * @param sort The order of the result
   * @return The user tasks
   */
  List<UserTask> findByWorkflowIdAndEndedAtIsNull(
      String workflowId,
      Sort sort);

  /**
   * The user tasks of one workflow which have ended.
   *
   * @param workflowId The id of the workflow
   * @param sort The order of the result
   * @return The user tasks
   */
  List<UserTask> findByWorkflowIdAndEndedAtIsNotNull(
      String workflowId,
      Sort sort);

}
