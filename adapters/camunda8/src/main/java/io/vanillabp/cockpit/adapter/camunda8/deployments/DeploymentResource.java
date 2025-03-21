package io.vanillabp.cockpit.adapter.camunda8.deployments;

import java.util.List;

public interface DeploymentResource {

    int getFileId();

    String getResourceName();

    <D extends Deployment> List<D> getDeployments();

    byte[] getResource();

}
