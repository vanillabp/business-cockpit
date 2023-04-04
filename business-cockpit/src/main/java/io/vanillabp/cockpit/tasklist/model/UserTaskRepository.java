package io.vanillabp.cockpit.tasklist.model;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTaskRepository extends MongoRepository<UserTask, String> {

    @Query(value = "{ }", fields = "{ '_id': 1 }")
    List<UserTask> findAllIds(Pageable pageable);
    
}
