package io.vanillabp.cockpit.devshell.simulator.businesscockpit.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowModuleRepository extends JpaRepository<WorkflowModule, String> {
}
