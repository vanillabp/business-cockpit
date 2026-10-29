package io.vanillabp.cockpit.tasklist.model;

import java.util.List;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Reads and writes the user tasks of the {@code usertask} collection.
 */
@Repository
public interface UserTaskRepository extends MongoRepository<UserTask, String> {

  /**
   * One user task per workflow module, which holds the module's id and URI. It is a way to find
   * every workflow module which has reported a user task.
   *
   * @return One user task per workflow module. Only the id and the URI of the module are set, and
   *         the id of the task holds the id of the module as well
   */
  @Aggregation({
      "{ $sort:{ workflowModuleId: 1, workflowModuleUri: 1 } }", "{ $group:{ _id: '$workflowModuleId', workflowModuleId: { '$first': '$workflowModuleId' }, workflowModuleUri: { '$first': '$workflowModuleUri' } } }"
  })
  List<UserTask> findAllWorkflowModulesAndUris();

}
