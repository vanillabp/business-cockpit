package io.vanillabp.cockpit.workflowmodules.model;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowModuleRepository extends ReactiveMongoRepository<WorkflowModule, String> {
}
