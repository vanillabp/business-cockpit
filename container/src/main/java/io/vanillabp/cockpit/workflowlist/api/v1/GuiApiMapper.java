package io.vanillabp.cockpit.workflowlist.api.v1;

import io.vanillabp.cockpit.commons.mapstruct.NoMappingMethod;
import io.vanillabp.cockpit.gui.api.v1.Workflow;
import io.vanillabp.cockpit.gui.api.v1.Workflows;
import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper(implementationName = "WorkflowListGuiApiMapperImpl")
public abstract class GuiApiMapper {

    @Mapping(target = "uiUri", expression = "java(proxiedUiUri(workflow))")
    @Mapping(target = "workflowModuleUri", expression = "java(proxiedWorkflowModuleUri(workflow))")
    @Mapping(target = "id", source = "workflowId")
    public abstract Workflow toApi(
            io.vanillabp.cockpit.workflowlist.model.Workflow workflow);

    public abstract List<Workflow> toApi(List<io.vanillabp.cockpit.workflowlist.model.Workflow> data);

    @Mapping(target = "page.number", source = "data.number")
    @Mapping(target = "page.size", source = "data.size")
    @Mapping(target = "page.totalPages", source = "data.totalPages")
    @Mapping(target = "page.totalElements", source = "data.totalElements")
    @Mapping(target = "workflows", expression = "java(toApi(data.getContent()))")
    @Mapping(target = "serverTimestamp", source = "timestamp")
    public abstract Workflows toApi(Page<io.vanillabp.cockpit.workflowlist.model.Workflow> data,
                                    OffsetDateTime timestamp);

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

    @NoMappingMethod
    protected String proxiedWorkflowModuleUri(
            final io.vanillabp.cockpit.workflowlist.model.Workflow workflow) {
        
        if (workflow.getWorkflowModuleUri() == null) {
            return null;
        }
        
        return MicroserviceProxyRegistry.WORKFLOW_MODULES_PATH_PREFIX
                + workflow.getWorkflowModule();
        
    }
    
}
