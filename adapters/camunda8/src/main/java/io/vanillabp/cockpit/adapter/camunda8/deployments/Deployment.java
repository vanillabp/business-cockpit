package io.vanillabp.cockpit.adapter.camunda8.deployments;

import java.time.OffsetDateTime;

public interface Deployment {

    long getDefinitionKey();

    int getVersion();

    int getPackageId();

    OffsetDateTime getPublishedAt();

    <R extends DeploymentResource> R getDeployedResource();

}
