package io.vanillabp.cockpit.tasklist.api;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.gui.api.v1.GuiEvent;
import io.vanillabp.cockpit.gui.api.v1.UpdateEmitter;
import io.vanillabp.cockpit.gui.api.v1.UpdateStreamAudience;
import io.vanillabp.cockpit.gui.api.v1.UserTaskEvent;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.UserTaskVisibility;
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
 * Decides which update streams learn about a changed user task. It asks MongoDB once per stream,
 * with the views of that stream and the ids collected in the tick, and reads nothing but the ids.
 * The criteria are the ones the task list is built from, so a stream gets exactly the tasks one of
 * its lists could show.
 */
public class UserTaskStreamAudience implements UpdateStreamAudience {

    public static final String KIND_OF_ENTITY = "UserTask";

    /** The type of the wake-up call which makes a task list load everything it shows again. */
    public static final String RELOAD = "RELOAD";

    private final UserTaskService userTaskService;

    private final MongoTemplate mongoTemplate;

    public UserTaskStreamAudience(
            final UserTaskService userTaskService,
            final MongoTemplate mongoTemplate) {

        this.userTaskService = userTaskService;
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
     * Of these ids, the ones at least one of the views lets through. Ended tasks count as well,
     * because a list of ended tasks shows them.
     */
    public Set<String> visibleThroughAnyOf(
            final Collection<Object> views,
            final Collection<String> ids) {

        final var criteriaOfEachView = views
                .stream()
                .filter(UserTaskVisibility.class::isInstance)
                .map(UserTaskVisibility.class::cast)
                .map(visibility -> userTaskService.buildUserTasksCriteria(
                        visibility,
                        null,
                        UserTaskService.RetrieveItemsMode.All,
                        List.of(Criteria.where("id").in(ids))))
                .toList();
        if (criteriaOfEachView.isEmpty()) {
            return Set.of();
        }

        final var query = new Query(criteriaOfEachView.size() == 1
                ? criteriaOfEachView.getFirst()
                : new Criteria().orOperator(criteriaOfEachView));
        query.fields().include("_id");

        return mongoTemplate
                .find(query, UserTask.class)
                .stream()
                .map(UserTask::getId)
                .collect(Collectors.toSet());

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
