package io.vanillabp.cockpit.workflowlist.api.v1;

import io.vanillabp.cockpit.commons.mapstruct.NoMappingMethod;
import io.vanillabp.cockpit.gui.api.v1.KwicResult;
import io.vanillabp.cockpit.gui.api.v1.SearchQuery;
import io.vanillabp.cockpit.gui.api.v1.Workflow;
import io.vanillabp.cockpit.gui.api.v1.Workflows;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupApiMapper;
import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import java.time.OffsetDateTime;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

@Mapper(implementationName = "WorkflowListGuiApiMapperImpl")
public abstract class GuiApiMapper {

    private static final String PERSON_MAPPING = "personMapping";
    private static final String GROUP_MAPPING = "groupMapping";

    @Autowired
    private PersonAndGroupApiMapper personAndGroupMapper;

    @Named(PERSON_MAPPING)
    public io.vanillabp.cockpit.gui.api.v1.Person toPerson(
            final Person user) {
        return personAndGroupMapper.personToApiPerson(user);
    }

    @Named(GROUP_MAPPING)
    public io.vanillabp.cockpit.gui.api.v1.Group toGroup(
            final Group group) {
        return personAndGroupMapper.groupToApiGroup(group);
    }

    @Mapping(target = "uiUri", expression = "java(proxiedUiUri(workflow))")
    @Mapping(target = "workflowModuleUri", expression = "java(proxiedWorkflowModuleUri(workflow))")
    @Mapping(target = "id", source = "workflowId")
    @Mapping(target = "initiator", source = "initiator", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToUsers", source = "accessibleToUsers", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToGroups", source = "accessibleToGroups", qualifiedByName = GROUP_MAPPING)
    public abstract Workflow toApi(
            io.vanillabp.cockpit.workflowlist.model.Workflow workflow);

    public abstract List<Workflow> toApi(List<io.vanillabp.cockpit.workflowlist.model.Workflow> data);

    @Mapping(target = "page.number", source = "data.number")
    @Mapping(target = "page.size", source = "data.size")
    @Mapping(target = "page.totalPages", source = "data.totalPages")
    @Mapping(target = "page.totalElements", source = "data.totalElements")
    @Mapping(target = "workflows", expression = "java(toApi(data.getContent()))")
    @Mapping(target = "serverTimestamp", source = "timestamp")
    @Mapping(target = "requestId", source = "requestId")
    public abstract Workflows toApi(Page<io.vanillabp.cockpit.workflowlist.model.Workflow> data,
                                    OffsetDateTime timestamp,
                                    String requestId);

    public abstract io.vanillabp.cockpit.util.SearchQuery toModel(SearchQuery data);

    public abstract List<io.vanillabp.cockpit.util.SearchQuery> toModel(List<SearchQuery> data);

    @NoMappingMethod
    protected String proxiedUiUri(
            final io.vanillabp.cockpit.workflowlist.model.Workflow workflow) {
        
        if (workflow.getWorkflowModuleId() == null) {
            return null;
        }
        if (workflow.getUiUriPath() == null) {
            return null;
        }
        
        return MicroserviceProxyRegistry.WORKFLOW_MODULES_PATH_PREFIX
                + workflow.getWorkflowModuleId()
                + (workflow.getUiUriPath().startsWith("/")
                        ? workflow.getUiUriPath()
                        : "/" + workflow.getUiUriPath());
        
    }

    @NoMappingMethod
    protected String proxiedWorkflowModuleUri(
            final io.vanillabp.cockpit.workflowlist.model.Workflow workflow) {
        
        if (workflow.getWorkflowModuleId() == null) {
            return null;
        }
        
        return MicroserviceProxyRegistry.WORKFLOW_MODULES_PATH_PREFIX
                + workflow.getWorkflowModuleId();
        
    }

    public abstract KwicResult toApi(WorkflowlistService.KwicResult result);

}
