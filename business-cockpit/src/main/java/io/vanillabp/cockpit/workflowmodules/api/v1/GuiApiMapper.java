package io.vanillabp.cockpit.workflowmodules.api.v1;

import io.vanillabp.cockpit.gui.api.v1.WorkflowModule;
import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(implementationName = "WorkflowModulesGuiApiMapperImpl")
public abstract class GuiApiMapper {

    @Mapping(target = "uri", expression = "java(uriMapper(model))")
    public abstract WorkflowModule toApi(io.vanillabp.cockpit.workflowmodules.model.WorkflowModule model);

    public String uriMapper(
            final io.vanillabp.cockpit.workflowmodules.model.WorkflowModule module) {

        if (module == null) {
            return null;
        }
        if (module.getUri() == null) {
            return null;
        }
        return MicroserviceProxyRegistry.WORKFLOW_MODULES_PATH_PREFIX + module.getId();

    }

}
