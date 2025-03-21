package io.vanillabp.cockpit.adapter.camunda8.deployments.mongodb;

import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = DeploymentResource.COLLECTION_NAME)
public class DeployedBpmn extends DeploymentResource
        implements io.vanillabp.cockpit.adapter.camunda8.deployments.DeployedBpmn {

}
