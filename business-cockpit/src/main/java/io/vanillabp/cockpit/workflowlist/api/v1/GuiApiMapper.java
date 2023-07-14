package io.vanillabp.cockpit.workflowlist.api.v1;

import java.util.List;

import io.vanillabp.cockpit.commons.mapstruct.NoMappingMethod;
import io.vanillabp.cockpit.gui.api.v1.Workflow;
import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(implementationName = "WorkflowListGuiApiMapperImpl")
public abstract class GuiApiMapper {

    @Mapping(target = "uiUri", expression = "java(proxiedUiUri(workflow))")
    @Mapping(target = "id", source = "workflowId")
    public abstract Workflow toApi(
            io.vanillabp.cockpit.workflowlist.model.Workflow workflow);

    public abstract List<Workflow> toApi(
            List<io.vanillabp.cockpit.workflowlist.model.Workflow> workflows);

    @NoMappingMethod
    protected String proxiedUiUri(
            final io.vanillabp.cockpit.workflowlist.model.Workflow workflow) {

        if (workflow.getWorkflowModuleUri() == null) {
            return null;
        }
        if (workflow.getUiUriPath() == null) {
            return null;
        }

        return MicroserviceProxyRegistry.WORKFLOW_MODULES_PATH_PREFIX
                + workflow.getWorkflowModule()
                + (workflow.getUiUriPath().startsWith("/")
                        ? workflow.getUiUriPath()
                        : "/" + workflow.getUiUriPath());

    }

}
