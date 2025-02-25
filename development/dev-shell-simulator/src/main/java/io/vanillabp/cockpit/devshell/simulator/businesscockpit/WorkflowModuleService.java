package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.bpms.api.v1.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.gui.api.v1.WorkflowModule;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkflowModuleService {

    private final Map<String, WorkflowModule> workflowModules = new HashMap<>();

    @Autowired
    private OfficialApiMapper mapper;

    public void registerWorkflowModule(
            final String workflowModuleId,
            final RegisterWorkflowModuleEvent event) {

        if ((workflowModuleId == null) || (event == null)) {
            throw new IllegalArgumentException("Workflow Module ID or event must not be null!");
        }

        final var workflowModule = mapper.toApi(event, workflowModuleId);
        workflowModules.put(workflowModuleId, workflowModule);

    }

    public WorkflowModule getWorkflowModule(
            final String workflowModuleId) {

        if (workflowModuleId == null) {
            throw new IllegalArgumentException("Workflow Module ID must not be null");
        }

        return workflowModules.get(workflowModuleId);

    }

}
