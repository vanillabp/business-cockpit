package io.vanillabp.cockpit.adapter.camunda8.deployments.jpa;

import io.vanillabp.cockpit.adapter.camunda8.deployments.ProcessInstance;
import io.vanillabp.cockpit.adapter.camunda8.deployments.ProcessInstancePersistence;
import java.util.List;
import java.util.Optional;

public class JpaProcessInstancePersistence implements ProcessInstancePersistence {

    private final ProcessInstanceRepository processInstanceRepository;

    public JpaProcessInstancePersistence(
            final ProcessInstanceRepository processInstanceRepository) {

        this.processInstanceRepository = processInstanceRepository;

    }

    @Override
    public void save(
            long processInstanceKey,
            String businessKey,
            String bpmnProcessId,
            Long version,
            Long processDefinitionKey,
            String tenantId) {

        final var processInstance = new ProcessInstanceEntity();
        processInstance.setProcessInstanceKey(processInstanceKey);
        processInstance.setBpmnProcessId(bpmnProcessId);
        processInstance.setVersion(version);
        processInstance.setBusinessKey(businessKey);
        processInstance.setProcessDefinitionKey(processDefinitionKey);
        processInstance.setTenantId(tenantId);

        processInstanceRepository
                .save(processInstance);

    }

    @Override
    public Optional<? extends ProcessInstance> findById(
            final long processInstanceKey) {

        return processInstanceRepository
                .findById(processInstanceKey);

    }

    @Override
    public List<? extends ProcessInstance> findByBusinessKey(
            final String businessKey) {

        return processInstanceRepository
                .findByBusinessKey(businessKey);

    }

}
