package io.vanillabp.cockpit.devshell.simulator.businesscockpit.model;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, String> {

    List<Workflow> findByEndedAtIsNull(Sort sort);

    List<Workflow> findByEndedAtIsNotNull(Sort sort);

}
