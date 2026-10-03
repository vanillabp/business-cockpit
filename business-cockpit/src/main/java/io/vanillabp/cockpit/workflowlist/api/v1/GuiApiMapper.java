package io.vanillabp.cockpit.workflowlist.api.v1;

import io.vanillabp.cockpit.commons.mapstruct.NoMappingMethod;
import io.vanillabp.cockpit.gui.api.v1.KwicResult;
import io.vanillabp.cockpit.gui.api.v1.SearchQuery;
import io.vanillabp.cockpit.gui.api.v1.Workflow;
import io.vanillabp.cockpit.gui.api.v1.WorkflowRetrieveMode;
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
import org.mapstruct.ValueMapping;
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

    @ValueMapping(target = "All", source = "ALL")
    @ValueMapping(target = "Active", source = "ACTIVE")
    @ValueMapping(target = "Inactive", source = "INACTIVE")
    public abstract WorkflowlistService.RetrieveItemsMode toModel(WorkflowRetrieveMode mode);

    /**
     * What the cockpit knows about one workflow, for its own user interface.
     * <p>
     * <code>uiUri</code> carries the path the workflow module reported, unchanged. Turning a path
     * into an address is a convention between the module and the user interface which loads it, so
     * the cockpit does not read <code>uiUriType</code> and builds nothing from it.
     *
     * @param workflow The workflow as it is stored
     * @return The workflow as the GUI API answers it
     */
    @Mapping(target = "uiUri", source = "uiUriPath")
    @Mapping(target = "workflowModuleUri", expression = "java(proxiedWorkflowModuleUri(workflow))")
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
    protected String proxiedWorkflowModuleUri(
            final io.vanillabp.cockpit.workflowlist.model.Workflow workflow) {
        
        if (workflow.getWorkflowModuleId() == null) {
            return null;
        }
        
        return MicroserviceProxyRegistry.WORKFLOW_MODULES_PATH_PREFIX
                + workflow.getWorkflowModuleId();
        
    }

    public abstract KwicResult toApi(io.vanillabp.cockpit.util.kwic.KwicResult result);

}
