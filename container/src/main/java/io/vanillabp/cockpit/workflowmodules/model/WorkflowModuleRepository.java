package io.vanillabp.cockpit.workflowmodules.model;

import java.util.List;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;

@Repository
public interface WorkflowModuleRepository extends ReactiveMongoRepository<WorkflowModule, String> {
    // IntelliJ thinks we need a Collection of Lists here, but we want intersecting results here
    Flux<WorkflowModule> findByPermittedRolesNullOrPermittedRolesIn(List<String> permittedRoles);
}
