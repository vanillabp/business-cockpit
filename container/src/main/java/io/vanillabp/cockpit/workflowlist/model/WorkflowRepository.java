package io.vanillabp.cockpit.workflowlist.model;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface WorkflowRepository extends ReactiveMongoRepository<Workflow, String> {

    @Aggregation({
            "{ $sort:{ workflowModuleId: 1, workflowModuleUri: 1 } }",
            "{ $group:{ _id: '$workflowModuleId', workflowModuleId: { '$first': '$workflowModuleId' }, workflowModuleUri: { '$first': '$workflowModuleUri' } } }"
        })
    Flux<Workflow> findAllWorkflowModulesAndUris();

}
