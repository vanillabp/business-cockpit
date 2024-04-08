package io.vanillabp.cockpit.tasklist.model;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface UserTaskRepository extends ReactiveMongoRepository<UserTask, String> {

    @Aggregation({
            "{ $sort:{ workflowModuleId: 1, workflowModuleUri: 1 } }",
            "{ $group:{ _id: '$workflowModuleId', workflowModuleId: { '$first': '$workflowModuleId' }, workflowModuleUri: { '$first': '$workflowModuleUri' } } }"
        })
    Flux<UserTask> findAllWorkflowModulesAndUris();
    
}
