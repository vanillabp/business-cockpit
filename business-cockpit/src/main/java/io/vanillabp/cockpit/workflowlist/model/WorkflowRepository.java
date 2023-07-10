package io.vanillabp.cockpit.workflowlist.model;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowRepository extends ReactiveMongoRepository<Workflow, String> {

}
