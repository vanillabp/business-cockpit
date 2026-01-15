package io.vanillabp.cockpit.devshell.simulator.businesscockpit.model;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTaskRepository extends JpaRepository<UserTask, String> {

    List<UserTask> findByEndedAtIsNull();

    List<UserTask> findByEndedAtIsNotNull();

    List<UserTask> findByWorkflowId(String workflowId);

    List<UserTask> findByWorkflowIdAndEndedAtIsNull(String workflowId);

    List<UserTask> findByWorkflowIdAndEndedAtIsNotNull(String workflowId);

}
