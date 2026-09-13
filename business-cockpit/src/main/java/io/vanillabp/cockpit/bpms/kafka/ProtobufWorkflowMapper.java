package io.vanillabp.cockpit.bpms.kafka;

import com.google.protobuf.Timestamp;
import io.vanillabp.cockpit.bpms.WhatAnEndReports;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsMap;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.util.protobuf.ProtobufHelper;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import java.time.OffsetDateTime;
import java.util.Map;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
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
    // the event's timestamp, stamped where reports are weighed against each other:
    @Mapping(target = "latestEventAt", ignore = true)
    @Mapping(target = "createdAt", source = "timestamp")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "initiator", source = "initiator", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToUsers", source = "accessibleToUsersList", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToGroups", source = "accessibleToGroupsList", qualifiedByName = GROUP_MAPPING)
    // protoc deprecates the plain getter of a map field and points at the ...Map() one, the same way
    // it names a repeated field ...List. Naming the source explicitly keeps the deprecated getter out
    // of the generated mapper. Both getters return the same map.
    @Mapping(target = "title", source = "titleMap")
    @Mapping(target = "details", source = "details", qualifiedByName = DETAILS_MAPPING)
    public abstract Workflow toNewWorkflow(WorkflowCreatedOrUpdatedEvent event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    // maintained by the cockpit itself, never taken from an event:
    @Mapping(target = "reportedAt", ignore = true)
    // the event's timestamp, stamped where reports are weighed against each other:
    @Mapping(target = "latestEventAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "initiator", source = "initiator", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToUsers", source = "accessibleToUsersList", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToGroups", source = "accessibleToGroupsList", qualifiedByName = GROUP_MAPPING)
    @Mapping(target = "title", source = "titleMap")
    @Mapping(target = "details", source = "details", qualifiedByName = DETAILS_MAPPING)
    public abstract Workflow toUpdatedWorkflow(WorkflowCreatedOrUpdatedEvent event, @MappingTarget Workflow result);

    // an end overwrites what it reports and leaves the rest of the stored case as it is - see
    // decision 19 in the repository's DECISIONS.md. Who started the case is declared optional in the
    // protobuf schema and arrives as null, which is what this strategy answers; the title, the
    // business data and the permissions to open the case arrive empty instead, which
    // toEndedWorkflow answers.
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    // the record is the one the service looked up, and MongoDB counts its saves:
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    // maintained by the cockpit itself, never taken from an event:
    @Mapping(target = "reportedAt", ignore = true)
    // the event's timestamp, stamped where reports are weighed against each other:
    @Mapping(target = "latestEventAt", ignore = true)
    // an end does not say when the case began, and guessing it would make the same case look
    // different depending on which of the two reports arrived first:
    @Mapping(target = "createdAt", ignore = true)
    // audit information, replaced by the cockpit's own clock and user whenever the record is saved:
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    // the service sets this one, because an end means more to it than the event says
    @Mapping(target = "endedAt", ignore = true)
    // read from the users and groups the case is accessible to whenever they are asked for:
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "initiator", source = "initiator", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToUsers", source = "accessibleToUsersList", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToGroups", source = "accessibleToGroupsList", qualifiedByName = GROUP_MAPPING)
    @Mapping(target = "title", source = "titleMap")
    @Mapping(target = "details", source = "details", qualifiedByName = DETAILS_MAPPING)
    protected abstract Workflow mapEndedWorkflow(WorkflowCreatedOrUpdatedEvent event, @MappingTarget Workflow result);

    /**
     * Maps a completed or cancelled event onto the stored workflow, keeping the business data and
     * the title the cockpit already has where the end reports none.
     * <p>
     * The sender fills the details field whether it has anything to put in it or not, and a
     * protobuf map carries no presence information either way, so an empty map is what an end of a
     * case the BPMS can no longer describe looks like here. The title arrives the same way, and so do
     * the users and groups the case is accessible to, because a repeated protobuf field has no
     * presence information either. Mapping that last one would take everybody's permission to open a
     * finished case ({@link WhatAnEndReports}, and see decision 19 in the repository's DECISIONS.md).
     *
     * @param event The end as it was reported
     * @param result The stored workflow, changed in place
     * @return The stored workflow
     */
    public Workflow toEndedWorkflow(
            final WorkflowCreatedOrUpdatedEvent event,
            @MappingTarget final Workflow result) {

        final var storedDetails = WhatAnEndReports.before(result.getDetails());
        final var storedFulltextSearch = result.getDetailsFulltextSearch();
        final var storedTitle = WhatAnEndReports.before(result.getTitle());
        final var storedAccessibleToUsers = WhatAnEndReports.before(result.getAccessibleToUsers());
        final var storedAccessibleToGroups = WhatAnEndReports.before(result.getAccessibleToGroups());
        final var workflow = mapEndedWorkflow(event, result);
        workflow.setDetails(WhatAnEndReports.whatToStore(workflow.getDetails(), storedDetails));
        workflow.setDetailsFulltextSearch(
                WhatAnEndReports.whatToStore(workflow.getDetailsFulltextSearch(), storedFulltextSearch));
        workflow.setTitle(WhatAnEndReports.whatToStore(workflow.getTitle(), storedTitle));
        workflow.setAccessibleToUsers(
                WhatAnEndReports.whatToStore(workflow.getAccessibleToUsers(), storedAccessibleToUsers));
        workflow.setAccessibleToGroups(
                WhatAnEndReports.whatToStore(workflow.getAccessibleToGroups(), storedAccessibleToGroups));
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