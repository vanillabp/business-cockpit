package io.vanillabp.cockpit.tasklist.model;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public interface UserTaskRepository extends ReactiveMongoRepository<UserTask, String> {

    String QUERY_ACTIVE = "{ $or: [ { endedAt: null }, { endedAt: { $gte: :#{#endedSince} } } ] }";

    @Query(value = QUERY_ACTIVE, fields = "{ '_id': 1 }")
    Flux<UserTask> findIdsOfActive(@Param("endedSince") Instant endedSince, Pageable pageable);
    
    @Query(value = QUERY_ACTIVE)
    Flux<UserTask> findActive(@Param("endedSince") Instant endedSince, Pageable pageable);
    
    @Query(value = QUERY_ACTIVE, count = true)
    Mono<Long> countActive(@Param("endedSince") Instant endedSince);
    
    @Query(value = "{ workflowId: ?0 }", sort = "{ dueDate: 1, createdAt: 1, _id: 1 }")
    Flux<UserTask> findAllByWorkflowId(String workflowId);

    @Query(value = "{ $and: [ { workflowId: ?0 }, { endedAt: null } ] }", sort = "{ dueDate: 1, createdAt: 1, _id: 1 }")
    Flux<UserTask> findActiveByWorkflowId(String workflowId);

    @Aggregation({
            "{ $sort:{ workflowModule: 1, workflowModuleUri: 1 } }",
            "{ $group:{ _id: '$workflowModule', workflowModule: { '$first': '$workflowModule' }, workflowModuleUri: { '$first': '$workflowModuleUri' } } }"
        })
    Flux<UserTask> findAllWorkflowModulesAndUris();
    
}
