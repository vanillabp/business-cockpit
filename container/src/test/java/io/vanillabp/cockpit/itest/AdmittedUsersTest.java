package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A user task may name who else sees it. The workflow module writes that list as
 * {@code admittedUsers}, and whoever stands in it reaches the task without being a candidate for it
 * and keeps reaching it after the group which once brought them the task is taken away.
 *
 * <p>The withdrawal these tests run is the real one: a workflow module reports a group hierarchy,
 * the hierarchy is what puts the user into the group the task addresses, and re-registering the
 * module without that entry takes the group away again. It reaches the next request of a user who is
 * already logged in, so no second login is needed to see the effect.
 *
 * <p>These tests came from story 258, which answered the same question inside the cockpit by noting
 * who opened what. The cause is different now, the business question is the same, so they ask the
 * same things of the application.
 */
class AdmittedUsersTest extends ItestBase {

    /** Registrations need a URI, but no test here follows the proxy route to it. */
    private static final String SOME_MODULE_URI = "http://localhost:65004";

    private String moduleId;
    private String token;
    private String grantedGroup;
    private String cookieOfMartin;
    private String cookieOfPetra;

    @BeforeEach
    void registerModuleAndLogInBothUsers() {

        moduleId = unique("admitted-module");
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

    /** Lets both users reach the group the tasks of these tests address. */
    private void grantTheGroupToBoth() {

        final var response = bpmsV1_1("/workflow-module/" + moduleId, """
                {
                  "id": "%s",
                  "uri": "%s",
                  "taskProviderApiUriPath": "/task-provider/v1",
                  "workflowProviderApiUriPath": "/workflow-provider/v1",
                  "groupHierarchy": [
                    { "group": "%s", "targets": [ "%s" ] },
                    { "group": "%s", "targets": [ "%s" ] }
                  ]
                }
                """.formatted(
                        moduleId, SOME_MODULE_URI, GROUP_OF_MARTIN, grantedGroup, GROUP_OF_PETRA,
                        grantedGroup));
        assertThat(response.statusCode()).isEqualTo(200);

    }

    /**
     * Reports a task addressed to the group both users can reach through the hierarchy, with the
     * given extra properties spliced into the event.
     */
    private String createTask(
            final String extraProperties) {

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
                  "candidateGroups": [ "%s" ]%s
                }
                """.formatted(
                        unique("event"), userTaskId, isoNow(), moduleId, token, grantedGroup,
                        extraProperties));
        assertThat(response.statusCode()).isEqualTo(200);
        return userTaskId;

    }

    private String createTaskAdmitting(
            final String... userIds) {

        return createTask(",\n  \"admittedUsers\": [ \"" + String.join("\", \"", userIds) + "\" ]");

    }

    /** Reports the task again, this time naming who else may see it. */
    private void reportAgainAdmitting(
            final String userTaskId,
            final String userId) {

        final var response = bpmsV1_1("/usertask/" + userTaskId + "/updated", """
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
                  "candidateGroups": [ "%s" ],
                  "admittedUsers": [ "%s" ]
                }
                """.formatted(
                        unique("event"), userTaskId, isoNow(), moduleId, token, grantedGroup,
                        userId));
        assertThat(response.statusCode()).isEqualTo(200);

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

    @Test
    void anAdmittedUserKeepsTheTaskAfterTheRoleIsWithdrawn() {

        final var userTaskId = createTaskAdmitting(USER_MARTIN);
        await().untilAsserted(
                () -> assertThat(tasksListedFor(cookieOfMartin, "", "OpenTasks"))
                        .containsExactly(userTaskId));

        withdrawTheGroupFromMartin();

        await().untilAsserted(
                () -> assertThat(tasksListedFor(cookieOfPetra, "", "OpenTasks"))
                        .containsExactly(userTaskId));
        assertThat(tasksListedFor(cookieOfMartin, "", "OpenTasks")).containsExactly(userTaskId);
        assertThat(detailStatusFor(cookieOfMartin, userTaskId)).isEqualTo(200);

    }

    /**
     * The case of the story: the task is done, the role is gone, and the person the module admitted
     * can still read what they entered.
     */
    @Test
    void anAdmittedUserKeepsACompletedTask() {

        final var userTaskId = createTaskAdmitting(USER_MARTIN);
        await().untilAsserted(
                () -> assertThat(tasksListedFor(cookieOfMartin, "", "OpenTasks"))
                        .containsExactly(userTaskId));
        completeTask(userTaskId);

        withdrawTheGroupFromMartin();

        await().untilAsserted(
                () -> assertThat(tasksListedFor(cookieOfMartin, "", "ClosedTasksOnly"))
                        .containsExactly(userTaskId));
        assertThat(detailStatusFor(cookieOfMartin, userTaskId)).isEqualTo(200);

    }

    /** Being admitted is enough on its own: petra is in no group this task addresses. */
    @Test
    void anAdmittedUserSeesATaskWithoutBeingACandidate() {

        final var userTaskId = createTaskAdmitting(USER_PETRA);

        await().untilAsserted(
                () -> assertThat(tasksListedFor(cookieOfPetra, "", "OpenTasks"))
                        .containsExactly(userTaskId));
        assertThat(detailStatusFor(cookieOfPetra, userTaskId)).isEqualTo(200);

    }

    /** A task which admits nobody is gone with the role, which is what it was before this story. */
    @Test
    void aTaskAdmittingNobodyIsGoneWithTheRole() {

        final var userTaskId = createTask("");
        await().untilAsserted(
                () -> assertThat(tasksListedFor(cookieOfMartin, "", "OpenTasks"))
                        .containsExactly(userTaskId));

        withdrawTheGroupFromMartin();

        await().untilAsserted(
                () -> assertThat(tasksListedFor(cookieOfMartin, "", "OpenTasks")).isEmpty());
        assertThat(detailStatusFor(cookieOfMartin, userTaskId)).isEqualTo(404);

    }

    /** Admitting one person tells nobody else about the task. */
    @Test
    void admittingOneUserLeavesAnothersViewAlone() {

        final var userTaskId = createTaskAdmitting(USER_MARTIN);

        await().untilAsserted(
                () -> assertThat(tasksListedFor(cookieOfMartin, "", "OpenTasks"))
                        .containsExactly(userTaskId));
        assertThat(tasksListedFor(cookieOfPetra, "", "OpenTasks")).isEmpty();
        assertThat(detailStatusFor(cookieOfPetra, userTaskId)).isEqualTo(404);

    }

    /**
     * An exclusion holds against an admission. Keeping somebody out of a task is the stronger word
     * of the two, and a module which says both about the same person has contradicted itself.
     */
    @Test
    void anExclusionHoldsAgainstAnAdmission() {

        grantTheGroupToBoth();
        final var userTaskId = createTask("""
                ,
                  "admittedUsers": [ "martin" ],
                  "excludedCandidateUsers": [ "martin" ]""");

        await().untilAsserted(
                () -> assertThat(tasksListedFor(cookieOfPetra, "", "OpenTasks"))
                        .containsExactly(userTaskId));
        assertThat(tasksListedFor(cookieOfMartin, "", "OpenTasks")).isEmpty();
        assertThat(detailStatusFor(cookieOfMartin, userTaskId)).isEqualTo(404);

    }

    /**
     * The list of what a user's groups may take is about work offered to them, and a group they are
     * no longer in offers nothing. Being admitted does not put a task back into that list, while the
     * main list keeps it, which is where the person looks for what they had a hand in.
     */
    @Test
    void theListOfWorkUpForGrabsDoesNotOfferAnAdmittedTask() {

        final var userTaskId = createTaskAdmitting(USER_MARTIN);
        await().untilAsserted(
                () -> assertThat(tasksListedFor(cookieOfMartin, "/user-roles", "OpenTasks"))
                        .containsExactly(userTaskId));

        withdrawTheGroupFromMartin();

        await().untilAsserted(
                () -> assertThat(tasksListedFor(cookieOfMartin, "/user-roles", "OpenTasks"))
                        .isEmpty());
        assertThat(tasksListedFor(cookieOfMartin, "", "OpenTasks")).containsExactly(userTaskId);

    }

    /**
     * There is no retrofit for a task reported before the module filled the list. Reporting the task
     * again is what carries the admission, and nothing else does.
     */
    @Test
    void aLaterReportIsWhatAdmitsSomebody() {

        final var userTaskId = createTask("");
        await().untilAsserted(
                () -> assertThat(tasksListedFor(cookieOfMartin, "", "OpenTasks"))
                        .containsExactly(userTaskId));

        withdrawTheGroupFromMartin();
        await().untilAsserted(
                () -> assertThat(tasksListedFor(cookieOfMartin, "", "OpenTasks")).isEmpty());

        reportAgainAdmitting(userTaskId, USER_MARTIN);

        await().untilAsserted(
                () -> assertThat(tasksListedFor(cookieOfMartin, "", "OpenTasks"))
                        .containsExactly(userTaskId));
        assertThat(detailStatusFor(cookieOfMartin, userTaskId)).isEqualTo(200);

    }

}
