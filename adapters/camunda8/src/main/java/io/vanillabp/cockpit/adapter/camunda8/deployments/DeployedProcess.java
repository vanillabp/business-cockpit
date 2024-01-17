package io.vanillabp.cockpit.adapter.camunda8.deployments;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity(name = "BusinessCockpitDeployedProcess")
@DiscriminatorValue(DeployedProcess.TYPE)
public class DeployedProcess extends Deployment {
    
    public static final String TYPE = "PROCESS";
    
    /** the BPMN process id of the process */
    @Column(name = "BPMN_PROCESS_ID")
    private String bpmnProcessId;

    public String getBpmnProcessId() {
        return bpmnProcessId;
    }

    public void setBpmnProcessId(String bpmnProcessId) {
        this.bpmnProcessId = bpmnProcessId;
    }

}
