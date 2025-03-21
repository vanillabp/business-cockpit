package io.vanillabp.cockpit.adapter.camunda8.deployments;

import java.util.List;
import java.util.Optional;

public interface ProcessInstancePersistence {

    void save(
            long processInstanceKey,
            String businessKey,
            String bpmnProcessId,
            Long version,
            Long processDefinitionKey,
            String tenantId);

    Optional<? extends ProcessInstance> findById(long processInstanceKey);

    List<? extends ProcessInstance> findByBusinessKey(String businessKey);

}
