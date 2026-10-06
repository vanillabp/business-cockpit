package io.vanillabp.cockpit.workflowlist.api;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.gui.api.v1.GuiEvent;
import io.vanillabp.cockpit.gui.api.v1.UpdateEmitter;
import io.vanillabp.cockpit.gui.api.v1.UpdateStreamAudience;
import io.vanillabp.cockpit.gui.api.v1.WorkflowEvent;
import io.vanillabp.cockpit.workflowlist.WorkflowVisibility;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;

/**
 * Decides which update streams learn about a changed workflow. It works like
 * {@code UserTaskStreamAudience}: one query per stream, with the views of that stream and the ids
 * collected in the tick, built from the criteria of the workflow list.
 */
public class WorkflowStreamAudience implements UpdateStreamAudience {

    public static final String KIND_OF_ENTITY = "Workflow";

    /** The type of the wake-up call which makes a workflow list load everything it shows again. */
    public static final String RELOAD = "RELOAD";

    private final WorkflowlistService workflowlistService;

    private final MongoTemplate mongoTemplate;

    public WorkflowStreamAudience(
            final WorkflowlistService workflowlistService,
            final MongoTemplate mongoTemplate) {

        this.workflowlistService = workflowlistService;
        this.mongoTemplate = mongoTemplate;

    }

    @Override
    public String kindOfEntity() {

        return KIND_OF_ENTITY;

    }

    @Override
    public Object widestViewOf(
            final UserDetails user) {

        return WorkflowVisibility.workflowsAddressedTo(user);

    }

    @Override
    public Map<UpdateEmitter, Set<String>> whatEachStreamMaySee(
            final Collection<UpdateEmitter> streams,
            final Set<String> changedIds) {

        final var result = new HashMap<UpdateEmitter, Set<String>>();
        streams.forEach(stream -> {
            final var visible = visibleThroughAnyOf(stream.viewsOf(KIND_OF_ENTITY), changedIds);
            if (!visible.isEmpty()) {
                result.put(stream, visible);
            }
        });
        return result;

    }

    /**
     * Of these ids, the ones at least one of the views lets through. Ended workflows count as well,
     * because a list of ended workflows shows them.
     */
    public Set<String> visibleThroughAnyOf(
            final Collection<Object> views,
            final Collection<String> ids) {

        final var criteriaOfEachView = views
                .stream()
                .filter(WorkflowVisibility.class::isInstance)
                .map(WorkflowVisibility.class::cast)
                .map(visibility -> workflowlistService.buildWorkflowlistCriteria(
                        visibility,
                        null,
                        WorkflowlistService.RetrieveItemsMode.All,
                        List.of(Criteria.where("id").in(ids)),
                        null))
                .map(WorkflowStreamAudience::asCriteria)
                .toList();
        if (criteriaOfEachView.isEmpty()) {
            return Set.of();
        }

        final var query = new Query(criteriaOfEachView.size() == 1
                ? criteriaOfEachView.getFirst()
                : new Criteria().orOperator(criteriaOfEachView));
        query.fields().include("_id");

        return mongoTemplate
                .find(query, Workflow.class)
                .stream()
                .map(Workflow::getId)
                .collect(Collectors.toSet());

    }

    /**
     * The workflow list declares its criteria as a {@link CriteriaDefinition}, and an {@code $or}
     * needs them as {@link Criteria}. What it builds is always one, which this checks rather than
     * assumes.
     */
    private static Criteria asCriteria(
            final CriteriaDefinition definition) {

        if (definition instanceof Criteria criteria) {
            return criteria;
        }
        throw new IllegalStateException(
                "The criteria of the workflow list are a " + definition.getClass().getName()
                + " and not a Criteria, so they cannot be combined for the update stream");

    }

    @Override
    public GuiEvent reloadEvent() {

        return new GuiEvent(
                KIND_OF_ENTITY,
                null,
                new WorkflowEvent()
                        .name(KIND_OF_ENTITY)
                        .id("")
                        .type(RELOAD));

    }

}
