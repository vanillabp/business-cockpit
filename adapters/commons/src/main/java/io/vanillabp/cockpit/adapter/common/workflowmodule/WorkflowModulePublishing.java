package io.vanillabp.cockpit.adapter.common.workflowmodule;

import io.vanillabp.cockpit.adapter.common.workflowmodule.events.WorkflowModuleEvent;

public interface WorkflowModulePublishing {

    void publish(WorkflowModuleEvent event);

}
