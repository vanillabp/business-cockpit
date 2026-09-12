package io.vanillabp.cockpit.tasklist.model;

import java.time.OffsetDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A note that one person once opened one user task. The cockpit writes it while a full view of a
 * task is answered and reads it back as a reason to keep showing that task, so somebody who worked
 * on a task can still read what they entered after the role which brought them the task is gone.
 *
 * <p>Listing a task writes nothing. Only a request for the whole task does, which is what keeps a
 * person from landing in here for every row they scrolled past.
 *
 * <p>The note carries the workflow module, the workflow and the task definition of the task it is
 * about. That is what lets the cockpit answer whether the setting is on for a note without reading
 * the task itself, and a note is therefore readable on its own.
 */
@Document(collection = OpenedUserTask.COLLECTION_NAME)
public class OpenedUserTask {

    public static final String COLLECTION_NAME = "openedusertask";

    /** The pair this note is about, written as {@code <user task id>#<user id>}. */
    @Id
    private String id;

    private String userTaskId;

    private String userId;

    private OffsetDateTime openedAt;

    private String workflowModuleId;

    private String bpmnProcessId;

    private String taskDefinition;

    /** The id of the note about one user and one task, which is the same for every later view. */
    public static String idOf(
            final String userTaskId,
            final String userId) {

        return userTaskId + "#" + userId;

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserTaskId() {
        return userTaskId;
    }

    public void setUserTaskId(String userTaskId) {
        this.userTaskId = userTaskId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public OffsetDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(OffsetDateTime openedAt) {
        this.openedAt = openedAt;
    }

    public String getWorkflowModuleId() {
        return workflowModuleId;
    }

    public void setWorkflowModuleId(String workflowModuleId) {
        this.workflowModuleId = workflowModuleId;
    }

    public String getBpmnProcessId() {
        return bpmnProcessId;
    }

    public void setBpmnProcessId(String bpmnProcessId) {
        this.bpmnProcessId = bpmnProcessId;
    }

    public String getTaskDefinition() {
        return taskDefinition;
    }

    public void setTaskDefinition(String taskDefinition) {
        this.taskDefinition = taskDefinition;
    }

}
