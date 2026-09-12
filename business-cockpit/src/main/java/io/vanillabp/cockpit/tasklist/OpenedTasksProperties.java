package io.vanillabp.cockpit.tasklist;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Whether a user task somebody opened stays visible to them once the role which brought them the
 * task is gone. On by default, and written like this when it is not wanted everywhere:
 *
 * <pre>
 * business-cockpit:
 *   opened-tasks-stay-visible: true
 *   workflow-modules:
 *     taxi-ride:
 *       opened-tasks-stay-visible: false
 *       workflows:
 *         TaxiRide:
 *           opened-tasks-stay-visible: true
 *           user-tasks:
 *             approve:
 *               opened-tasks-stay-visible: false
 * </pre>
 *
 * The four levels are the ones a workflow module writes its own keys in, and they are read the same
 * way: the most specific value wins, and a level which says nothing is a level answered by the one
 * above it. What differs is where they are written. A workflow module says what it reports, while
 * this says what the cockpit shows, so it is answered in the cockpit's own configuration.
 *
 * <p>The value is read while a task is opened and again while a list is built, so switching it off
 * stops new notes from being written and stops the ones already written from counting. Switching it
 * on again brings back what was noted before it was switched off.
 */
@ConfigurationProperties(prefix = "business-cockpit", ignoreUnknownFields = true)
public class OpenedTasksProperties {

    /** What holds for every workflow module which says nothing of its own. */
    private boolean openedTasksStayVisible = true;

    /** What single workflow modules say, by workflow module id. */
    private Map<String, WorkflowModule> workflowModules = Map.of();

    /**
     * Whether the given user task stays visible to whoever opened it.
     *
     * @param workflowModuleId The workflow module the task belongs to
     * @param bpmnProcessId    The workflow the task belongs to
     * @param taskDefinition   The task definition, which is the task's name in the BPMN file
     * @return What the most specific of the four levels says
     */
    public boolean openedTasksStayVisible(
            final String workflowModuleId,
            final String bpmnProcessId,
            final String taskDefinition) {

        final var module = workflowModules.get(workflowModuleId);
        if (module == null) {
            return openedTasksStayVisible;
        }
        final var workflow = module.getWorkflows().get(bpmnProcessId);
        if (workflow == null) {
            return module.getOpenedTasksStayVisible() != null
                    ? module.getOpenedTasksStayVisible()
                    : openedTasksStayVisible;
        }
        final var userTask = workflow.getUserTasks().get(taskDefinition);
        if ((userTask != null)
                && (userTask.getOpenedTasksStayVisible() != null)) {
            return userTask.getOpenedTasksStayVisible();
        }
        if (workflow.getOpenedTasksStayVisible() != null) {
            return workflow.getOpenedTasksStayVisible();
        }
        return module.getOpenedTasksStayVisible() != null
                ? module.getOpenedTasksStayVisible()
                : openedTasksStayVisible;

    }

    public boolean isOpenedTasksStayVisible() {
        return openedTasksStayVisible;
    }

    public void setOpenedTasksStayVisible(boolean openedTasksStayVisible) {
        this.openedTasksStayVisible = openedTasksStayVisible;
    }

    public Map<String, WorkflowModule> getWorkflowModules() {
        return workflowModules;
    }

    public void setWorkflowModules(Map<String, WorkflowModule> workflowModules) {
        this.workflowModules = workflowModules;
    }

    /** What one workflow module says, and what its workflows say. */
    public static class WorkflowModule {

        private Boolean openedTasksStayVisible;

        private Map<String, Workflow> workflows = Map.of();

        public Boolean getOpenedTasksStayVisible() {
            return openedTasksStayVisible;
        }

        public void setOpenedTasksStayVisible(Boolean openedTasksStayVisible) {
            this.openedTasksStayVisible = openedTasksStayVisible;
        }

        public Map<String, Workflow> getWorkflows() {
            return workflows;
        }

        public void setWorkflows(Map<String, Workflow> workflows) {
            this.workflows = workflows;
        }

    }

    /** What one workflow says, and what its user tasks say. */
    public static class Workflow {

        private Boolean openedTasksStayVisible;

        private Map<String, UserTask> userTasks = Map.of();

        public Boolean getOpenedTasksStayVisible() {
            return openedTasksStayVisible;
        }

        public void setOpenedTasksStayVisible(Boolean openedTasksStayVisible) {
            this.openedTasksStayVisible = openedTasksStayVisible;
        }

        public Map<String, UserTask> getUserTasks() {
            return userTasks;
        }

        public void setUserTasks(Map<String, UserTask> userTasks) {
            this.userTasks = userTasks;
        }

    }

    /** What one user task says. */
    public static class UserTask {

        private Boolean openedTasksStayVisible;

        public Boolean getOpenedTasksStayVisible() {
            return openedTasksStayVisible;
        }

        public void setOpenedTasksStayVisible(Boolean openedTasksStayVisible) {
            this.openedTasksStayVisible = openedTasksStayVisible;
        }

    }

}
