package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A person who worked on a task keeps it, even after the group which brought them the task is taken
 * away. That is a question of the business rather than of the code, and it has to be answered in
 * one place instead of in every workflow module, which is why the cockpit notes who opened what.
 *
 * <p>The withdrawal these tests run is the real one: a workflow module reports a group hierarchy,
 * the hierarchy is what puts the user into the group the task addresses, and re-registering the
 * module without that entry takes the group away again. It reaches the next request of a user who
 * is already logged in, so no second login is needed to see the effect.
 *
 * <p>The setting itself is exercised in {@link OpenedTasksStayVisibleSettingTest}, which starts the
 * application with all four levels of it written.
 */
class SeeingATaskYouOpenedTest extends ItestBase {

    /** Registrations need a URI, but no test here follows the proxy route to it. */
    private static final String SOME_MODULE_URI = "http://localhost:65004";

    private String moduleId;
    private String token;
    private String grantedGroup;
    private String cookieOfMartin;
    private String cookieOfPetra;

    @BeforeEach
    void registerModuleAndLogInBothUsers() {

        moduleId = unique("opened-module");
        token = unique("token");
        grantedGroup = unique("ride-approver");
        cookieOfMartin = loginToGui(USER_MARTIN);
        cookieOfPetra = loginToGui(USER_PETRA);
        grantTheGroupTo(GROUP_OF_MARTIN);

    }

    /**
     * Registers the module saying that members of the given group are also members of the group the
     * tasks of these tests address. Registering again with another group is how the role is
     * withdrawn.
     */
    private void grantTheGroupTo(
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

    /** Takes the group away from martin by handing it to somebody else. */
    private void withdrawTheGroupFromMartin() {

        grantTheGroupTo(GROUP_OF_PETRA);

    }

    private String createTask() {

        final var userTaskId = unique("task");
        final var response = bpmsV1_1("/usertask/created", """
                {
                  "id": "%s",
                  "userTaskId": "%s",
                  "timestamp": "%s",
                  "workflowModuleId": "%s",
                  "bpmnProcessId": "taxi-ride",
                  "title": { "en": "Do ride" },
                  "taskDefinition": "do-ride",
                  "uiUriPath": "/remoteEntry.js",
                  "uiUriType": "WEBPACK_MF_REACT",
                  "detailsFulltextSearch": "%s",
                  "candidateGroups": [ "%s" ]
                }
                """.formatted(unique("event"), userTaskId, isoNow(), moduleId, token, grantedGroup));
        assertThat(response.statusCode()).isEqualTo(200);
        return userTaskId;

    }

    private void completeTask(
            final String userTaskId) {

        final var response = bpmsV1_1("/usertask/" + userTaskId + "/completed", """
                {
                  "id": "%s",
                  "userTaskId": "%s",
                  "timestamp": "%s",
                  "initiator": "martin",
                  "workflowModuleId": "%s",
                  "bpmnProcessId": "taxi-ride",
                  "title": { "en": "Do ride" },
                  "taskDefinition": "do-ride",
                  "uiUriPath": "/remoteEntry.js",
                  "uiUriType": "WEBPACK_MF_REACT",
                  "detailsFulltextSearch": "%s"
                }
                """.formatted(unique("event"), userTaskId, isoNow(), moduleId, token));
        assertThat(response.statusCode()).isEqualTo(200);

    }

    @SuppressWarnings("unchecked")
    private List<String> tasksListedFor(
            final String cookie,
            final String listPath,
            final String mode) {

        final var response = guiPost(cookie, listPath + "/usertask", """
                {
                  "pageNumber": 0,
                  "pageSize": 50,
                  "searchQueries": [ { "query": "%s" } ],
                  "sortAscending": true,
                  "mode": "%s"
                }
                """.formatted(token, mode));
        assertThat(response.statusCode()).isEqualTo(200);
        return json(response).read("$.userTasks[*].id", List.class);

    }

    private int detailStatusFor(
            final String cookie,
            final String userTaskId) {

        return guiGet(cookie, "/usertask/" + userTaskId).statusCode();

    }

    /** Fetching the whole task is what the user interface does when somebody opens it. */
    private void openTask(
            final String cookie,
            final String userTaskId) {

        assertThat(detailStatusFor(cookie, userTaskId)).isEqualTo(200);

    }

    @Test
    void aTaskStaysVisibleForWhoeverOpenedItAfterTheRoleIsWithdrawn() {

        final var userTaskId = createTask();
        await().untilAsserted(
                () -> assertThat(tasksListedFor(cookieOfMartin, "", "OpenTasks"))
                        .containsExactly(userTaskId));
        openTask(cookieOfMartin, userTaskId);

        withdrawTheGroupFromMartin();

        await().untilAsserted(
                () -> assertThat(tasksListedFor(cookieOfPetra, "", "OpenTasks"))
                        .containsExactly(userTaskId));
        assertThat(tasksListedFor(cookieOfMartin, "", "OpenTasks")).containsExactly(userTaskId);
        assertThat(detailStatusFor(cookieOfMartin, userTaskId)).isEqualTo(200);

    }

    /**
     * The case of the story: the task is done, the role is gone, and the person who worked on it
     * can still read what they entered.
     */
    @Test
    void aCompletedTaskStaysVisibleForWhoeverOpenedIt() {

        final var userTaskId = createTask();
        await().untilAsserted(
                () -> assertThat(tasksListedFor(cookieOfMartin, "", "OpenTasks"))
                        .containsExactly(userTaskId));
        openTask(cookieOfMartin, userTaskId);
        completeTask(userTaskId);

        withdrawTheGroupFromMartin();

        await().untilAsserted(
                () -> assertThat(tasksListedFor(cookieOfMartin, "", "ClosedTasksOnly"))
                        .containsExactly(userTaskId));
        assertThat(detailStatusFor(cookieOfMartin, userTaskId)).isEqualTo(200);

    }

    /**
     * Scrolling past a task is not working on it. Only a request for the whole task is noted, so a
     * long list leaves nobody in the collection of who saw what.
     */
    @Test
    void aTaskOnlySeenInAListIsGoneWithTheRole() {

        final var userTaskId = createTask();
        await().untilAsserted(
                () -> assertThat(tasksListedFor(cookieOfMartin, "", "OpenTasks"))
                        .containsExactly(userTaskId));

        withdrawTheGroupFromMartin();

        await().untilAsserted(
                () -> assertThat(tasksListedFor(cookieOfMartin, "", "OpenTasks")).isEmpty());
        assertThat(detailStatusFor(cookieOfMartin, userTaskId)).isEqualTo(404);

    }

    /** Somebody else's view is untouched: opening a task tells nobody else about it. */
    @Test
    void aTaskOpenedByOneUserStaysHiddenFromAnother() {

        final var userTaskId = createTask();
        await().untilAsserted(
                () -> assertThat(tasksListedFor(cookieOfMartin, "", "OpenTasks"))
                        .containsExactly(userTaskId));
        openTask(cookieOfMartin, userTaskId);

        assertThat(tasksListedFor(cookieOfPetra, "", "OpenTasks")).isEmpty();
        assertThat(detailStatusFor(cookieOfPetra, userTaskId)).isEqualTo(404);

    }

    /**
     * The list of what a user's groups may take is about work offered to them, and a group they are
     * no longer in offers nothing. So an earlier view does not put a task back into that list, while
     * the main list keeps it, which is where the person looks for what they had a hand in.
     */
    @Test
    void theListOfWorkUpForGrabsDoesNotOfferATaskWhoseRoleIsGone() {

        final var userTaskId = createTask();
        await().untilAsserted(
                () -> assertThat(tasksListedFor(cookieOfMartin, "/user-roles", "OpenTasks"))
                        .containsExactly(userTaskId));
        openTask(cookieOfMartin, userTaskId);

        withdrawTheGroupFromMartin();

        await().untilAsserted(
                () -> assertThat(tasksListedFor(cookieOfMartin, "/user-roles", "OpenTasks"))
                        .isEmpty());
        assertThat(tasksListedFor(cookieOfMartin, "", "OpenTasks")).containsExactly(userTaskId);

    }

}
