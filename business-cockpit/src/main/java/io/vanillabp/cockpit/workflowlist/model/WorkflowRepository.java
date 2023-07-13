package io.vanillabp.cockpit.workflowlist.model;

import io.vanillabp.cockpit.tasklist.model.UserTask;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface WorkflowRepository extends ReactiveMongoRepository<Workflow, String> {

    @Query(value = "{ endedAt: null }")
    Flux<Workflow> findAllBy(Pageable pageable);

    @Query(value = "{ endedAt: null }", fields = "{ '_id': 1, 'workflowId': 1 }")
    Flux<Workflow> findAllIds(Pageable pageable);

    @Query(value = "{ endedAt: null }", count = true)
    Mono<Long> countAll();

}
