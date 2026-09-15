package io.vanillabp.cockpit.bpms.api.v1_1;

import io.vanillabp.cockpit.bpms.WhatAnEndReports;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import java.util.function.Supplier;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(implementationName = "WorkflowMapperV1_1Impl")
public abstract class WorkflowMapper {

    private static final String PERSON_MAPPING = "personMapping";
    private static final String GROUP_MAPPING = "groupMapping";

    @Autowired
    private PersonAndGroupMapper personAndGroupMapper;

    @Named(PERSON_MAPPING)
    public Person toPerson(
            final String userId) {
        // a reporting system which names no user means no person at all. A person built from a
        // null id is hidden by the GUI, but every comparison of ids stumbles over it
        return userId == null ? null : personAndGroupMapper.toModelPerson(userId);
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
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "initiator", source = "initiator", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToUsers", source = "accessibleToUsers", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToGroups", source = "accessibleToGroups", qualifiedByName = GROUP_MAPPING)
    public abstract Workflow toNewWorkflow(WorkflowCreatedEvent event);

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
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "initiator", source = "initiator", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToUsers", source = "accessibleToUsers", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToGroups", source = "accessibleToGroups", qualifiedByName = GROUP_MAPPING)
    public abstract Workflow toNewWorkflow(WorkflowUpdatedEvent event);

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
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "initiator", source = "initiator", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToUsers", source = "accessibleToUsers", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToGroups", source = "accessibleToGroups", qualifiedByName = GROUP_MAPPING)
    public abstract Workflow toUpdatedWorkflow(WorkflowUpdatedEvent event, @MappingTarget Workflow result);

    // an end overwrites what it reports and leaves the rest of the stored case as it is. See
    // decision 19 in the repository's DECISIONS.md
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    // the record is the one the service looked up, and MongoDB counts its saves:
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    // maintained by the cockpit itself, never taken from an event:
    @Mapping(target = "reportedAt", ignore = true)
    // the event's timestamp, stamped where reports are weighed against each other:
    @Mapping(target = "latestEventAt", ignore = true)
    // an end does not say when the case began. Guessing it would make the same case look
    // different, depending on which of the two reports arrived first:
    @Mapping(target = "createdAt", ignore = true)
    // audit information, replaced by the cockpit's own clock and user whenever the record is saved:
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    // the service sets this one, because an end means more to it than the event says
    @Mapping(target = "endedAt", ignore = true)
    // read from the users and groups the case is accessible to whenever they are asked for:
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "initiator", source = "initiator", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToUsers", source = "accessibleToUsers", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToGroups", source = "accessibleToGroups", qualifiedByName = GROUP_MAPPING)
    protected abstract Workflow mapEndedWorkflow(WorkflowCompletedEvent event, @MappingTarget Workflow result);

    // an end overwrites what it reports and leaves the rest of the stored case as it is. See
    // decision 19 in the repository's DECISIONS.md
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    // the record is the one the service looked up, and MongoDB counts its saves:
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    // maintained by the cockpit itself, never taken from an event:
    @Mapping(target = "reportedAt", ignore = true)
    // the event's timestamp, stamped where reports are weighed against each other:
    @Mapping(target = "latestEventAt", ignore = true)
    // an end does not say when the case began. Guessing it would make the same case look
    // different, depending on which of the two reports arrived first:
    @Mapping(target = "createdAt", ignore = true)
    // audit information, replaced by the cockpit's own clock and user whenever the record is saved:
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    // the service sets this one, because an end means more to it than the event says
    @Mapping(target = "endedAt", ignore = true)
    // read from the users and groups the case is accessible to whenever they are asked for:
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "initiator", source = "initiator", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToUsers", source = "accessibleToUsers", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToGroups", source = "accessibleToGroups", qualifiedByName = GROUP_MAPPING)
    protected abstract Workflow mapEndedWorkflow(WorkflowCancelledEvent event, @MappingTarget Workflow result);

    /**
     * Maps a completed or cancelled event onto the stored workflow, so that the list of finished
     * cases shows what the case ended with.
     * <p>
     * Whatever the end does not report leaves what is stored alone (see decision 19 in the
     * repository's DECISIONS.md). {@link WhatAnEndReports} holds the part of that rule which a
     * mapping annotation cannot express. Field by field the mapping reads like this.
     * <p>
     * An end may replace what it reports, because that is the youngest answer there is about the
     * case. This covers where the case sits in the model: the source, the workflow module, the BPMN
     * process and its version, and the business id. It covers what the cockpit shows of the case:
     * the title, the address of the user interface, the comment, the business data and the words
     * the fulltext search reads. And it covers who started the case and who may see it.
     * <p>
     * An end may not replace what it cannot know. When the cockpit stored the case and how often
     * the record was saved is the cockpit's own record-keeping. When the case began is something an
     * end does not say. That the case has ended is set by the service and not mapped, because an
     * end means more to the service than the event says. The target groups and the dangling flag
     * are read from the users and groups the case is accessible to.
     *
     * @param event The end as it was reported
     * @param result The stored workflow, changed in place
     * @return The stored workflow
     */
    public Workflow toEndedWorkflow(
            final WorkflowCompletedEvent event,
            @MappingTarget final Workflow result) {

        return keepingWhatTheEndDoesNotReport(result, () -> mapEndedWorkflow(event, result));

    }

    /**
     * The same for a cancellation. The API declares a schema per kind of event and the generator
     * made a class of each, so the two ends share no common type.
     *
     * @param event The end as it was reported
     * @param result The stored workflow, changed in place
     * @return The stored workflow
     */
    public Workflow toEndedWorkflow(
            final WorkflowCancelledEvent event,
            @MappingTarget final Workflow result) {

        return keepingWhatTheEndDoesNotReport(result, () -> mapEndedWorkflow(event, result));

    }

    private static Workflow keepingWhatTheEndDoesNotReport(
            final Workflow stored,
            final Supplier<Workflow> mapping) {

        final var storedDetails = WhatAnEndReports.before(stored.getDetails());
        final var storedFulltextSearch = stored.getDetailsFulltextSearch();
        final var storedTitle = WhatAnEndReports.before(stored.getTitle());
        final var storedAccessibleToUsers = WhatAnEndReports.before(stored.getAccessibleToUsers());
        final var storedAccessibleToGroups = WhatAnEndReports.before(stored.getAccessibleToGroups());
        final var workflow = mapping.get();
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

}
