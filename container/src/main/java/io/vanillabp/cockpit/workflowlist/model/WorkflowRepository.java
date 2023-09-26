package io.vanillabp.cockpit.workflowlist.model;

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
public interface WorkflowRepository extends ReactiveMongoRepository<Workflow, String> {

    String QUERY_ACTIVE = "{ $or: [ { endedAt: null }, { endedAt: { $gte: :#{#endedSince} } } ] }";
    
    @Query(value = QUERY_ACTIVE)
    Flux<Workflow> findActive(@Param("endedSince") Instant endedSince, Pageable pageable);

    @Query(value = QUERY_ACTIVE, fields = "{ '_id': 1, 'workflowId': 1 }")
    Flux<Workflow> findIdsOfActive(@Param("endedSince") Instant endedSince, Pageable pageable);

    @Query(value = QUERY_ACTIVE, count = true)
    Mono<Long> countActive(@Param("endedSince") Instant endedSince);

    @Aggregation({
        "{ $sort:{ workflowModule: 1, workflowModuleUri: 1 } }",
        "{ $group:{ _id: '$workflowModule', workflowModule: { '$first': '$workflowModule' }, workflowModuleUri: { '$first': '$workflowModuleUri' } } }"
    })
    Flux<Workflow> findAllWorkflowModulesAndUris();

}
