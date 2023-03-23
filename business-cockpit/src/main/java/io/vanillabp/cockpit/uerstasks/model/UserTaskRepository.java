package io.vanillabp.cockpit.uerstasks.model;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTaskRepository extends MongoRepository<UserTask, String> {

}
