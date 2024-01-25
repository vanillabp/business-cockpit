package io.vanillabp.cockpit.adapter.camunda8.workflow;

import java.util.Optional;

public class LifeCycleEventInformation {
    private String id;

    private String deleteReason;

    private String businessKey;

    private String processInstanceId;



    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeleteReason() {
        return deleteReason;
    }

    public void setDeleteReason(String deleteReason) {
        this.deleteReason = deleteReason;
    }

    public Optional<String> getProcessInstanceId() {
        return Optional.ofNullable(processInstanceId);
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }
}
