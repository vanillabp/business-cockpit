package io.vanillabp.cockpit.workflowlist.model;

import java.util.List;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowRepository extends MongoRepository<Workflow, String> {

    @Aggregation({
            "{ $sort:{ workflowModuleId: 1, workflowModuleUri: 1 } }",
            "{ $group:{ _id: '$workflowModuleId', workflowModuleId: { '$first': '$workflowModuleId' }, workflowModuleUri: { '$first': '$workflowModuleUri' } } }"
        })
    List<Workflow> findAllWorkflowModulesAndUris();

}
