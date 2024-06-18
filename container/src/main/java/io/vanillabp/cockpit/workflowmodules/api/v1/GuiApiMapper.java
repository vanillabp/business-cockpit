package io.vanillabp.cockpit.workflowmodules.api.v1;

import io.vanillabp.cockpit.gui.api.v1.WorkflowModule;
import org.mapstruct.Mapper;

@Mapper(implementationName = "WorkflowModulesGuiApiMapperImpl")
public interface GuiApiMapper {

    WorkflowModule toApi(io.vanillabp.cockpit.workflowmodules.model.WorkflowModule model);

}
