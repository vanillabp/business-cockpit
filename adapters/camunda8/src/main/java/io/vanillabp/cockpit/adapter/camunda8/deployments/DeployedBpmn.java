package io.vanillabp.cockpit.adapter.camunda8.deployments;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity(name = "BusinessCockpitDeployedBpmn")
@DiscriminatorValue(DeployedBpmn.TYPE)
public class DeployedBpmn extends DeploymentResource {

    public static final String TYPE = "BPMN";
    
}
