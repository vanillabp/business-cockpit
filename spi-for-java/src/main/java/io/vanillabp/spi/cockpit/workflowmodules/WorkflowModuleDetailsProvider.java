package io.vanillabp.spi.cockpit.workflowmodules;

import java.util.List;

public interface WorkflowModuleDetailsProvider {

    List<String> getAccessibleToGroups();

    String getWorkflowModuleId();

}
