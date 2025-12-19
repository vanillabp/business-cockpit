package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowModule;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowModuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkflowModuleService {

    @Autowired
    private WorkflowModuleRepository workflowModules;

    public void registerWorkflowModule(
            final String workflowModuleId,
            final WorkflowModule workflowModule) {

        if ((workflowModuleId == null) || (workflowModule == null)) {
            throw new IllegalArgumentException("Workflow Module ID or event must not be null!");
        }

        workflowModules.save(workflowModule);

    }

    public WorkflowModule getWorkflowModule(
            final String workflowModuleId) {

        if (workflowModuleId == null) {
            throw new IllegalArgumentException("Workflow Module ID must not be null");
        }

        return workflowModules
                .findById(workflowModuleId)
                .orElseThrow(() -> new IllegalStateException("Workflow module with ID " + workflowModuleId + " not found"));

    }

}
