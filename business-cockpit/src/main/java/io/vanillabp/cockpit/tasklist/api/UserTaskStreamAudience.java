package io.vanillabp.cockpit.tasklist.api;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.gui.api.v1.GuiEvent;
import io.vanillabp.cockpit.gui.api.v1.UpdateEmitter;
import io.vanillabp.cockpit.gui.api.v1.UpdateStreamAudience;
import io.vanillabp.cockpit.gui.api.v1.UserTaskEvent;
import io.vanillabp.cockpit.tasklist.UserTaskVisibility;
import io.vanillabp.cockpit.tasklist.UserTaskVisibility.TaskFacts;
import io.vanillabp.cockpit.tasklist.model.UserTask;
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
 * Decides which update streams learn about a changed user task. Per filtering tick it reads the
 * changed tasks once, with only the fields the visibility looks at, and then asks every stream's
 * views in memory. So the database sees one query per tick, however many tabs are open.
 */
public class UserTaskStreamAudience implements UpdateStreamAudience {

    public static final String KIND_OF_ENTITY = "UserTask";

    /** The type of the wake-up call which makes a task list load everything it shows again. */
    public static final String RELOAD = "RELOAD";

    private final MongoTemplate mongoTemplate;

    public UserTaskStreamAudience(
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

        return UserTaskVisibility.everythingTheUserMayWorkOn(user);

    }

    @Override
    public Map<UpdateEmitter, Set<String>> whatEachStreamMaySee(
            final Collection<UpdateEmitter> streams,
            final Set<String> changedIds) {

        final var changedTasks = factsOf(changedIds);
        final var result = new HashMap<UpdateEmitter, Set<String>>();
        if (changedTasks.isEmpty()) {
            return result;
        }
        streams.forEach(stream -> {
            final var views = stream
                    .viewsOf(KIND_OF_ENTITY)
                    .stream()
                    .filter(UserTaskVisibility.class::isInstance)
                    .map(UserTaskVisibility.class::cast)
                    .toList();
            final var visible = changedTasks
                    .stream()
                    .filter(task -> views.stream().anyMatch(view -> view.letsThrough(task)))
                    .map(TaskFacts::id)
                    .collect(Collectors.toSet());
            if (!visible.isEmpty()) {
                result.put(stream, visible);
            }
        });
        return result;

    }

    /**
     * The changed tasks, with the fields the visibility reads. A task deleted in the meantime is
     * missing, and only a stream whose browser shows it learns about it.
     */
    public List<TaskFacts> factsOf(
            final Collection<String> ids) {

        final var query = new Query(Criteria.where("id").in(ids));
        TaskFacts.fieldNames().forEach(query.fields()::include);
        return mongoTemplate.find(query, TaskFacts.class, UserTask.COLLECTION_NAME);

    }

    @Override
    public GuiEvent reloadEvent() {

        return new GuiEvent(
                KIND_OF_ENTITY,
                null,
                new UserTaskEvent()
                        .name(KIND_OF_ENTITY)
                        .id("")
                        .type(RELOAD));

    }

}
