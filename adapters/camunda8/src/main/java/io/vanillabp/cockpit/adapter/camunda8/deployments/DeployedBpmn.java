package io.vanillabp.cockpit.adapter.camunda8.deployments;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(DeployedBpmn.TYPE)
public class DeployedBpmn extends DeploymentResource {

    public static final String TYPE = "BPMN";
    
}
