package io.vanillabp.cockpit.workflowlist.api;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.gui.api.v1.GuiEvent;
import io.vanillabp.cockpit.gui.api.v1.UpdateEmitter;
import io.vanillabp.cockpit.gui.api.v1.UpdateStreamAudience;
import io.vanillabp.cockpit.gui.api.v1.WorkflowEvent;
import io.vanillabp.cockpit.workflowlist.WorkflowVisibility;
import io.vanillabp.cockpit.workflowlist.WorkflowVisibility.WorkflowFacts;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * Decides which update streams learn about a changed workflow. Per filtering tick it reads the
 * changed workflows once, with only the fields the visibility looks at, and then asks every stream's
 * views in memory. So the database sees one query per tick, however many tabs are open.
 */
public class WorkflowStreamAudience implements UpdateStreamAudience {

    public static final String KIND_OF_ENTITY = "Workflow";

    /** The type of the wake-up call which makes a workflow list load everything it shows again. */
    public static final String RELOAD = "RELOAD";

    private final MongoTemplate mongoTemplate;

    public WorkflowStreamAudience(
            final MongoTemplate mongoTemplate) {

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

        final var changedWorkflows = factsOf(changedIds);
        final var result = new HashMap<UpdateEmitter, Set<String>>();
        if (changedWorkflows.isEmpty()) {
            return result;
        }
        streams.forEach(stream -> {
            final var views = stream
                    .viewsOf(KIND_OF_ENTITY)
                    .stream()
                    .filter(WorkflowVisibility.class::isInstance)
                    .map(WorkflowVisibility.class::cast)
                    .toList();
            final var visible = changedWorkflows
                    .stream()
                    .filter(workflow -> views.stream().anyMatch(view -> view.letsThrough(workflow)))
                    .map(WorkflowFacts::id)
                    .collect(Collectors.toSet());
            if (!visible.isEmpty()) {
                result.put(stream, visible);
            }
        });
        return result;

    }

    /**
     * The changed workflows, with the fields the visibility reads. A workflow deleted in the meantime is
     * missing, and only a stream whose browser shows it learns about it.
     */
    public List<WorkflowFacts> factsOf(
            final Collection<String> ids) {

        final var query = new Query(Criteria.where("id").in(ids));
        WorkflowFacts.fieldNames().forEach(query.fields()::include);
        return mongoTemplate.find(query, WorkflowFacts.class, Workflow.COLLECTION_NAME);

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
