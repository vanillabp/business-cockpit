package io.vanillabp.cockpit.workflowmodules.model;

import java.util.List;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;

@Repository
public interface WorkflowModuleRepository extends ReactiveMongoRepository<WorkflowModule, String> {

    @Query("{ '$or': [ " +
            "  { 'accessibleToGroups': { $in: ?0 } }, " +
            "  { 'accessibleToGroups': { $exists: false } }, " +
            "  { 'accessibleToGroups': { $size: 0 } } " +
            "] }")
    Flux<WorkflowModule> findByPermittedAccessibleToGroups(List<String> groups);
}
