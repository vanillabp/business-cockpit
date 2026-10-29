package io.vanillabp.cockpit.workflowlist.model;

import java.util.List;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Reads and writes the workflows of the {@code workflow} collection.
 */
@Repository
public interface WorkflowRepository extends MongoRepository<Workflow, String> {

    /**
     * One workflow per workflow module, which holds the module's id and URI. It is a way to find
     * every workflow module which has reported a workflow.
     *
     * @return One workflow per workflow module. Only the id and the URI of the module are set, and
     *         the id of the workflow holds the id of the module as well
     */
    @Aggregation({
            "{ $sort:{ workflowModuleId: 1, workflowModuleUri: 1 } }",
            "{ $group:{ _id: '$workflowModuleId', workflowModuleId: { '$first': '$workflowModuleId' }, workflowModuleUri: { '$first': '$workflowModuleUri' } } }"
        })
    List<Workflow> findAllWorkflowModulesAndUris();

}
