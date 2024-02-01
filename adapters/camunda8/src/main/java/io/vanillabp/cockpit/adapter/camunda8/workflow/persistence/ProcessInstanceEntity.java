package io.vanillabp.cockpit.adapter.camunda8.workflow.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "CAMUNDA8_BC_PROCESS_INSTANCES")
public class ProcessInstanceEntity {

    @Id
    @Column(name = "PROCESS_INSTANCE_KEY")
    private long processInstanceKey;

    @Column(name = "BUSINESS_KEY")
    private String businessKey;

    public ProcessInstanceEntity(){
    };

    public ProcessInstanceEntity(long processInstanceKey, String businessKey) {
        this.processInstanceKey = processInstanceKey;
        this.businessKey = businessKey;
    }

    public long getProcessInstanceKey() {
        return processInstanceKey;
    }

    public void setProcessInstanceKey(long processInstanceKey) {
        this.processInstanceKey = processInstanceKey;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }
}
