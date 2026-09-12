package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * Whether a task somebody opened stays visible to them is a question of the business, so it is
 * written in the configuration, on four levels. This application says no globally, yes for one
 * workflow module, no again for one of its workflows and yes for one user task of that workflow. So
 * every level is the one which decides for exactly one of the tasks below, and the more specific one
 * wins each time.
 *
 * <p>What each test does is the run of {@link SeeingATaskYouOpenedTest}: martin gets a group through
 * a hierarchy, opens the task, and the group is handed to somebody else. What differs is only
 * whether the task is still his afterwards.
 */
@TestPropertySource(properties = {
        "business-cockpit.opened-tasks-stay-visible=false",
        "business-cockpit.workflow-modules.[itest-opened-on-module]"
                + ".opened-tasks-stay-visible=true",
        "business-cockpit.workflow-modules.[itest-opened-on-module].workflows.[taxi-ride]"
                + ".opened-tasks-stay-visible=false",
        "business-cockpit.workflow-modules.[itest-opened-on-module].workflows.[taxi-ride]"
                + ".user-tasks.[do-ride].opened-tasks-stay-visible=true"
})
class OpenedTasksStayVisibleSettingTest extends ItestBase {

    /** Registrations need a URI, but no test here follows the proxy route to it. */
    private static final String SOME_MODULE_URI = "http://localhost:65005";

    /** The workflow module the setting is written for, and the one it says nothing about. */
    private static final String MODULE_SAYING_YES = "itest-opened-on-module";
    private static final String MODULE_SAYING_NOTHING = "itest-opened-nothing-module";

    /** The workflow the setting is written for, and the one it says nothing about. */
    private static final String WORKFLOW_SAYING_NO = "taxi-ride";
    private static final String WORKFLOW_SAYING_NOTHING = "other-ride";

    /** The user task the setting is written for, and the one it says nothing about. */
    private static final String TASK_SAYING_YES = "do-ride";
    private static final String TASK_SAYING_NOTHING = "other-task";

    private String token;
    private String grantedGroup;
    private String cookieOfMartin;

    @BeforeEach
    void logInAndGrantTheGroupToMartin() {

        token = unique("token");
        grantedGroup = unique("ride-approver");
        cookieOfMartin = loginToGui(USER_MARTIN);

    }

    private void grantTheGroupTo(
            final String moduleId,
            final String group) {

        final var response = bpmsV1_1("/workflow-module/" + moduleId, """
                {
                  "id": "%s",
                  "uri": "%s",
                  "taskProviderApiUriPath": "/task-provider/v1",
                  "workflowProviderApiUriPath": "/workflow-provider/v1",
                  "groupHierarchy": [ { "group": "%s", "targets": [ "%s" ] } ]
                }
                """.formatted(moduleId, SOME_MODULE_URI, group, grantedGroup));
        assertThat(response.statusCode()).isEqualTo(200);

    }

    private String createTask(
            final String moduleId,
            final String bpmnProcessId,
            final String taskDefinition) {

        final var userTaskId = unique("task");
        final var response = bpmsV1_1("/usertask/created", """
                {
                  "id": "%s",
                  "userTaskId": "%s",
                  "timestamp": "%s",
                  "workflowModuleId": "%s",
                  "bpmnProcessId": "%s",
                  "title": { "en": "Do ride" },
                  "taskDefinition": "%s",
                  "uiUriPath": "/remoteEntry.js",
                  "uiUriType": "WEBPACK_MF_REACT",
                  "detailsFulltextSearch": "%s",
                  "candidateGroups": [ "%s" ]
                }
                """.formatted(unique("event"), userTaskId, isoNow(), moduleId, bpmnProcessId,
                        taskDefinition, token, grantedGroup));
        assertThat(response.statusCode()).isEqualTo(200);
        return userTaskId;

    }

    @SuppressWarnings("unchecked")
    private List<String> tasksListedForMartin() {

        final var response = guiPost(cookieOfMartin, "/usertask", """
                {
                  "pageNumber": 0,
                  "pageSize": 50,
                  "searchQueries": [ { "query": "%s" } ],
                  "sortAscending": true,
                  "mode": "OpenTasks"
                }
                """.formatted(token));
        assertThat(response.statusCode()).isEqualTo(200);
        return json(response).read("$.userTasks[*].id", List.class);

    }

    /**
     * The whole run of one task: martin reaches it through the group, opens it, and the group goes
     * to somebody else.
     *
     * @return The id of the task, to be asked for afterwards
     */
    private String openATaskAndLoseTheGroup(
            final String moduleId,
            final String bpmnProcessId,
            final String taskDefinition) {

        grantTheGroupTo(moduleId, GROUP_OF_MARTIN);
        final var userTaskId = createTask(moduleId, bpmnProcessId, taskDefinition);
        await().untilAsserted(() -> assertThat(tasksListedForMartin()).containsExactly(userTaskId));

        assertThat(guiGet(cookieOfMartin, "/usertask/" + userTaskId).statusCode()).isEqualTo(200);

        grantTheGroupTo(moduleId, GROUP_OF_PETRA);
        return userTaskId;

    }

    private void assertTaskIsStillMartins(
            final String userTaskId) {

        // the withdrawal reaches the next request, so the list is asked until it settles
        await().untilAsserted(() -> assertThat(tasksListedForMartin()).containsExactly(userTaskId));
        assertThat(guiGet(cookieOfMartin, "/usertask/" + userTaskId).statusCode()).isEqualTo(200);

    }

    private void assertTaskIsGoneFromMartin(
            final String userTaskId) {

        await().untilAsserted(() -> assertThat(tasksListedForMartin()).isEmpty());
        assertThat(guiGet(cookieOfMartin, "/usertask/" + userTaskId).statusCode()).isEqualTo(404);

    }

    @Test
    void theGlobalSettingDecidesForAWorkflowModuleWhichSaysNothing() {

        final var userTaskId = openATaskAndLoseTheGroup(
                MODULE_SAYING_NOTHING, WORKFLOW_SAYING_NOTHING, TASK_SAYING_NOTHING);

        assertTaskIsGoneFromMartin(userTaskId);

    }

    @Test
    void aWorkflowModuleOverridesTheGlobalSetting() {

        final var userTaskId = openATaskAndLoseTheGroup(
                MODULE_SAYING_YES, WORKFLOW_SAYING_NOTHING, TASK_SAYING_NOTHING);

        assertTaskIsStillMartins(userTaskId);

    }

    @Test
    void aWorkflowOverridesItsWorkflowModule() {

        final var userTaskId = openATaskAndLoseTheGroup(
                MODULE_SAYING_YES, WORKFLOW_SAYING_NO, TASK_SAYING_NOTHING);

        assertTaskIsGoneFromMartin(userTaskId);

    }

    @Test
    void aUserTaskOverridesItsWorkflow() {

        final var userTaskId = openATaskAndLoseTheGroup(
                MODULE_SAYING_YES, WORKFLOW_SAYING_NO, TASK_SAYING_YES);

        assertTaskIsStillMartins(userTaskId);

    }

}
