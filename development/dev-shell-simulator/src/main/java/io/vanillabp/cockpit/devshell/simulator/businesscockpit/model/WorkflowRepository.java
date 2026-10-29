package io.vanillabp.cockpit.devshell.simulator.businesscockpit.model;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Reads and writes the stored workflows.
 */
@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, String> {

  /**
   * The workflows which have not ended yet.
   *
   * @param sort The order of the result
   * @return The workflows
   */
  List<Workflow> findByEndedAtIsNull(
      Sort sort);

  /**
   * The workflows which have ended.
   *
   * @param sort The order of the result
   * @return The workflows
   */
  List<Workflow> findByEndedAtIsNotNull(
      Sort sort);

}
