package io.vanillabp.cockpit.devshell.simulator.businesscockpit.model;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTaskRepository extends JpaRepository<UserTask, String> {

    List<UserTask> findByEndedAtIsNull(Sort sort);

    List<UserTask> findByEndedAtIsNotNull(Sort sort);

    List<UserTask> findByWorkflowId(String workflowId, Sort sort);

    List<UserTask> findByWorkflowIdAndEndedAtIsNull(String workflowId, Sort sort);

    List<UserTask> findByWorkflowIdAndEndedAtIsNotNull(String workflowId, Sort sort);

}
