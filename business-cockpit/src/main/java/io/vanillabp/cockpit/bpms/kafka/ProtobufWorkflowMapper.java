package io.vanillabp.cockpit.bpms.kafka;

import com.google.protobuf.Timestamp;
import io.vanillabp.cockpit.bpms.DetailsOfAnEnd;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsMap;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.util.protobuf.ProtobufHelper;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import java.time.OffsetDateTime;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = MappingConstants.ComponentModel.DEFAULT)
public abstract class ProtobufWorkflowMapper {

    private static final String DETAILS_MAPPING = "detailsMapping";
    private static final String PERSON_MAPPING = "personMapping";
    private static final String GROUP_MAPPING = "groupMapping";

    @Autowired
    private PersonAndGroupMapper personAndGroupMapper;

    @Named(PERSON_MAPPING)
    public Person toPerson(
            final String userId) {
        return personAndGroupMapper.toModelPerson(userId);
    }

    @Named(GROUP_MAPPING)
    public Group toGroup(
            final String groupId) {
        return personAndGroupMapper.toModelGroup(groupId);
    }

    @Mapping(target = "id", source = "workflowId")
    @Mapping(target = "version", ignore = true)
    // maintained by the cockpit itself, never taken from an event:
    @Mapping(target = "reportedAt", ignore = true)
    @Mapping(target = "createdAt", source = "timestamp")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "initiator", source = "initiator", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToUsers", source = "accessibleToUsersList", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToGroups", source = "accessibleToGroupsList", qualifiedByName = GROUP_MAPPING)
    @Mapping(target = "details", source = "details", qualifiedByName = DETAILS_MAPPING)
    public abstract Workflow toNewWorkflow(WorkflowCreatedOrUpdatedEvent event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    // maintained by the cockpit itself, never taken from an event:
    @Mapping(target = "reportedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "initiator", source = "initiator", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToUsers", source = "accessibleToUsersList", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToGroups", source = "accessibleToGroupsList", qualifiedByName = GROUP_MAPPING)
    @Mapping(target = "details", source = "details", qualifiedByName = DETAILS_MAPPING)
    public abstract Workflow toUpdatedWorkflow(WorkflowCreatedOrUpdatedEvent event, @MappingTarget Workflow result);

    /**
     * Maps a completed or cancelled event onto the stored workflow, keeping the business data the
     * cockpit already has where the end reports none. {@link DetailsOfAnEnd} says why.
     *
     * @param event The end as it was reported
     * @param result The stored workflow, changed in place
     * @return The stored workflow
     */
    public Workflow toEndedWorkflow(
            final WorkflowCreatedOrUpdatedEvent event,
            @MappingTarget final Workflow result) {

        final var storedDetails = DetailsOfAnEnd.before(result.getDetails());
        final var storedFulltextSearch = result.getDetailsFulltextSearch();
        final var workflow = toUpdatedWorkflow(event, result);
        workflow.setDetails(DetailsOfAnEnd.whatToStore(workflow.getDetails(), storedDetails));
        workflow.setDetailsFulltextSearch(
                DetailsOfAnEnd.whatToStore(workflow.getDetailsFulltextSearch(), storedFulltextSearch));
        return workflow;

    }

    public OffsetDateTime map(Timestamp value) {
        return ProtobufHelper.map(value);
    }

    @Named(DETAILS_MAPPING)
    protected Map<String, Object> map(
            final DetailsMap detailsMap) {
        return DetailsMapper.mapMapValue(detailsMap);
    }

}