package io.vanillabp.cockpit.tasklist.model;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;

@Repository
public interface UserTaskRepository extends ReactiveMongoRepository<UserTask, String> {

    @Query(value = "{ endedAt: null }", fields = "{ '_id': 1 }")
    Flux<UserTask> findAllIds(Pageable pageable);
    
    @Query(value = "{ endedAt: null }")
    Flux<UserTask> findAllBy(Pageable pageable);
    
    @Aggregation({
            "{ $sort:{ workflowModule: 1, workflowModuleUri: 1 } }",
            "{ $group:{ _id: '$workflowModule', workflowModule: { '$first': '$workflowModule' }, workflowModuleUri: { '$first': '$workflowModuleUri' } } }"
        })
    Flux<UserTask> findAllWorkflowModulesAndUris();
    
}
