package io.vanillabp.cockpit.workflowlist.model;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Reads and writes the workflows of the {@code workflow} collection.
 */
@Repository
public interface WorkflowRepository extends MongoRepository<Workflow, String> {
}
