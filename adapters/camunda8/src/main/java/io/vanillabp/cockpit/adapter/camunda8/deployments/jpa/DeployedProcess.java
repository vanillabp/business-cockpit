package io.vanillabp.cockpit.adapter.camunda8.deployments.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity(name = "BusinessCockpitDeployedProcess")
@DiscriminatorValue(DeployedProcess.TYPE)
public class DeployedProcess extends Deployment
        implements io.vanillabp.cockpit.adapter.camunda8.deployments.DeployedProcess {
    
    public static final String TYPE = "PROCESS";
    
    /** the BPMN process id of the process */
    @Column(name = "C8D_BPMN_PROCESS_ID")
    private String bpmnProcessId;

    public String getBpmnProcessId() {
        return bpmnProcessId;
    }

    public void setBpmnProcessId(String bpmnProcessId) {
        this.bpmnProcessId = bpmnProcessId;
    }

}
