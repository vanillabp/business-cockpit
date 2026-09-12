package io.vanillabp.cockpit.tasklist;

import io.vanillabp.cockpit.tasklist.model.OpenedUserTask;
import io.vanillabp.cockpit.tasklist.model.OpenedUserTaskRepository;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/**
 * Who opened which user task, and what that is worth today. Both questions are asked against
 * {@link OpenedTasksProperties}: a task of a workflow the setting is off for is neither noted while
 * it is opened nor counted while a list is built, so switching the setting off stops the cockpit
 * from collecting and from using what it collected before.
 */
@Component
public class OpenedUserTasks {

    @Autowired
    private OpenedUserTaskRepository openedUserTasks;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private OpenedTasksProperties properties;

    /**
     * Notes that the given person looked at the whole task. The first view is the one kept, so a
     * person opening a task twice stays one note.
     */
    public void rememberOpening(
            final UserTask userTask,
            final String userId) {

        if (userId == null) {
            return;
        }
        if (!properties.openedTasksStayVisible(
                userTask.getWorkflowModuleId(),
                userTask.getBpmnProcessId(),
                userTask.getTaskDefinition())) {
            return;
        }

        final var note = new Update()
                .setOnInsert("userTaskId", userTask.getId())
                .setOnInsert("userId", userId)
                .setOnInsert("openedAt", OffsetDateTime.now())
                .setOnInsert("workflowModuleId", userTask.getWorkflowModuleId())
                .setOnInsert("bpmnProcessId", userTask.getBpmnProcessId())
                .setOnInsert("taskDefinition", userTask.getTaskDefinition());

        try {
            mongoTemplate.upsert(
                    new Query(Criteria
                            .where("_id")
                            .is(OpenedUserTask.idOf(userTask.getId(), userId))),
                    note,
                    OpenedUserTask.class);
        } catch (DuplicateKeyException e) {
            // two views of the same task at the same time, and the other one won
        }

    }

    /**
     * The ids of the tasks the given users opened and may still see. An id comes back only as long
     * as the setting of its workflow says so, which is why the note carries the workflow it belongs
     * to: answering this needs no second look at the tasks themselves.
     */
    public Collection<String> tasksOpenedBy(
            final Collection<String> userIds) {

        if ((userIds == null)
                || userIds.isEmpty()) {
            return List.of();
        }

        return openedUserTasks
                .findByUserIdIn(userIds)
                .stream()
                .filter(opened -> properties.openedTasksStayVisible(
                        opened.getWorkflowModuleId(),
                        opened.getBpmnProcessId(),
                        opened.getTaskDefinition()))
                .map(OpenedUserTask::getUserTaskId)
                .distinct()
                .toList();

    }

    /** Drops the notes about a user task which is gone, so they are kept as long as the task is. */
    public void forgetTask(
            final String userTaskId) {

        openedUserTasks.deleteByUserTaskId(userTaskId);

    }

}
