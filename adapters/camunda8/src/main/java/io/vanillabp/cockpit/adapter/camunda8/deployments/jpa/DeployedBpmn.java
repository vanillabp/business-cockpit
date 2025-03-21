package io.vanillabp.cockpit.adapter.camunda8.deployments.jpa;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity(name = "BusinessCockpitDeployedBpmn")
@DiscriminatorValue(DeployedBpmn.TYPE)
public class DeployedBpmn extends DeploymentResource
        implements io.vanillabp.cockpit.adapter.camunda8.deployments.DeployedBpmn {

    public static final String TYPE = "BPMN";
    
}
