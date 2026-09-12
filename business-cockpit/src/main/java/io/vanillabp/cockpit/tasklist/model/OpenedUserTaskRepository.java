package io.vanillabp.cockpit.tasklist.model;

import java.util.Collection;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OpenedUserTaskRepository extends MongoRepository<OpenedUserTask, String> {

    /** Every task the given users opened, which is what a list adds to what a role brings. */
    List<OpenedUserTask> findByUserIdIn(Collection<String> userIds);

    /** Drops the notes of a user task which is gone from the database. */
    void deleteByUserTaskId(String userTaskId);

}
