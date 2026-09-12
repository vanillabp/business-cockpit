package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A list says what a user may see, and everything naming a single task has to say the same. These
 * tests pin that down from the outside, because the two live in different places in the code: the
 * list is a Mongo query, the single task is one document, and only the visibility they are both
 * built from keeps them in step.
 *
 * <p>The rules:
 *
 * <ol>
 *   <li>Opening a task the list does not hold is answered as unknown, with the same status and the
 *       same body an id which was never reported gets.
 *   <li>Marking as read, claiming, assigning and setting a follow-up date on such a task change
 *       nothing and are answered as unknown as well.
 *   <li>The three delivered lists answer by their own rule, and so does what hangs below each of
 *       them, so the narrower view of a list is the narrower view of its task page too.
 *   <li>An application of its own defines a visibility none of the delivered ones matches and gets
 *       the same treatment for free. {@link GroupWideTasksGuiApiController} is the one this test
 *       reads against.
 * </ol>
 */
class UserTaskPermissionsTest extends ItestBase {

    /** Registrations need a URI, but no test here follows the proxy route to it. */
    private static final String SOME_MODULE_URI = "http://localhost:65003";

    private String moduleId;
    private String token;
    private String cookieOfMartin;
    private String cookieOfPetra;

    @BeforeEach
    void registerModuleAndLogInBothUsers() {

        moduleId = unique("ride-module");
        token = unique("token");
        registerWorkflowModule(moduleId, SOME_MODULE_URI);
        cookieOfMartin = loginToGui(USER_MARTIN);
        cookieOfPetra = loginToGui(USER_PETRA);

    }

    private String createTask(
            final String addressingFields) {

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
                  "detailsFulltextSearch": "%s"%s
                }
                """.formatted(unique("event"), userTaskId, isoNow(), moduleId, token,
                        addressingFields.isEmpty() ? "" : ",\n  " + addressingFields));
        assertThat(response.statusCode()).isEqualTo(200);
        return userTaskId;

    }

    private String createTaskForGroup(
            final String... groups) {

        return createTask("\"candidateGroups\": [ " + asJsonStrings(groups) + " ]");

    }

    private static String asJsonStrings(
            final String... values) {

        return Arrays
                .stream(values)
                .map(value -> "\"" + value + "\"")
                .collect(Collectors.joining(", "));

    }

    @SuppressWarnings("unchecked")
    private List<String> tasksListedFor(
            final String cookie,
            final String listPath) {

        final var response = guiPost(cookie, listPath + "/usertask", """
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

    private int detailStatusFor(
            final String cookie,
            final String listPath,
            final String userTaskId) {

        return guiGet(cookie, listPath + "/usertask/" + userTaskId).statusCode();

    }

    private String assigneeOf(
            final String cookie,
            final String userTaskId) {

        final var response = guiGet(cookie, "/usertask/" + userTaskId);
        assertThat(response.statusCode()).isEqualTo(200);
        return json(response).read("$.assignee.id", String.class);

    }

    // rule 1: the single task follows the list

    @Test
    void openingATaskHiddenFromTheListIsAnsweredAsUnknown() {

        final var userTaskId = createTaskForGroup(GROUP_OF_MARTIN);

        assertThat(tasksListedFor(cookieOfPetra, "")).isEmpty();
        assertThat(detailStatusFor(cookieOfMartin, "", userTaskId)).isEqualTo(200);
        assertThat(detailStatusFor(cookieOfPetra, "", userTaskId)).isEqualTo(404);

    }

    /**
     * The same answer as for a task which was never reported, so that trying ids tells a user
     * nothing about the tasks they are kept away from.
     */
    @Test
    void aHiddenAndAnUnknownTaskGetTheSameAnswer() {

        final var userTaskId = createTaskForGroup(GROUP_OF_MARTIN);

        final var hidden = guiGet(cookieOfPetra, "/usertask/" + userTaskId);
        final var unknown = guiGet(cookieOfPetra, "/usertask/" + unique("never-reported"));
        assertThat(hidden.statusCode()).isEqualTo(unknown.statusCode());
        assertThat(hidden.body()).isEqualTo(unknown.body());

    }

    /**
     * The exclusion list takes a task away from somebody who would otherwise be a candidate, and it
     * has to take it away from the task page as well. Otherwise four eyes end at the list.
     */
    @Test
    void aTaskExcludingTheUserIsAnsweredAsUnknownForThatUser() {

        final var userTaskId = createTask(
                "\"candidateGroups\": [ \"" + GROUP_OF_MARTIN + "\" ], "
                        + "\"excludedCandidateUsers\": [ \"" + USER_MARTIN + "\" ]");

        assertThat(tasksListedFor(cookieOfMartin, "")).isEmpty();
        assertThat(detailStatusFor(cookieOfMartin, "", userTaskId)).isEqualTo(404);

    }

    // rule 2: the actions follow the same rule and change nothing

    @Test
    void claimingATaskHiddenFromTheUserChangesNothing() {

        final var userTaskId = createTask(
                "\"candidateGroups\": [ \"" + GROUP_OF_MARTIN + "\" ], \"assignee\": \"martin\"");

        assertThat(guiPatch(cookieOfPetra, "/usertask/" + userTaskId + "/claim", null)
                .statusCode()).isEqualTo(404);
        assertThat(assigneeOf(cookieOfMartin, userTaskId)).isEqualTo(USER_MARTIN);

    }

    @Test
    void unclaimingATaskHiddenFromTheUserChangesNothing() {

        final var userTaskId = createTask(
                "\"candidateGroups\": [ \"" + GROUP_OF_MARTIN + "\" ], \"assignee\": \"martin\"");

        assertThat(guiPatch(cookieOfPetra, "/usertask/" + userTaskId + "/claim?unclaim=true", null)
                .statusCode()).isEqualTo(404);
        assertThat(assigneeOf(cookieOfMartin, userTaskId)).isEqualTo(USER_MARTIN);

    }

    @Test
    void assigningATaskHiddenFromTheUserChangesNothing() {

        final var userTaskId = createTaskForGroup(GROUP_OF_MARTIN);

        assertThat(guiPatch(cookieOfPetra,
                "/usertask/" + userTaskId + "/assign?userId=" + USER_PETRA, null)
                .statusCode()).isEqualTo(404);

        final var task = guiGet(cookieOfMartin, "/usertask/" + userTaskId);
        assertThat(task.statusCode()).isEqualTo(200);
        assertThat(json(task).read("$.candidateUsers", List.class)).isNull();

    }

    @Test
    void markingATaskHiddenFromTheUserAsReadChangesNothing() {

        final var userTaskId = createTaskForGroup(GROUP_OF_MARTIN);

        assertThat(guiPatch(cookieOfPetra, "/usertask/" + userTaskId + "/mark-as-read", null)
                .statusCode()).isEqualTo(404);

        // read state is kept per user, so martin's view is where a leaked mark would show up
        assertThat(json(guiGet(cookieOfMartin, "/usertask/" + userTaskId))
                .read("$.read", String.class)).isNull();

    }

    @Test
    void settingAFollowUpDateOnATaskHiddenFromTheUserChangesNothing() {

        final var userTaskId = createTaskForGroup(GROUP_OF_MARTIN);

        assertThat(guiPatch(cookieOfPetra, "/usertask/" + userTaskId + "/follow-up-date", """
                { "timestamp": "%s" }
                """.formatted(isoNow()))
                .statusCode()).isEqualTo(404);

        assertThat(json(guiGet(cookieOfMartin, "/usertask/" + userTaskId))
                .read("$.followUpDate", String.class)).isNull();

    }

    /**
     * The batch endpoints answer 200 whatever they found, the way they did before, so what is
     * pinned here is the effect rather than the status: the task of somebody else stays untouched
     * while the caller's own task in the same request is claimed.
     */
    @Test
    void claimingSeveralTasksSkipsTheOnesHiddenFromTheUser() {

        final var ownTask = createTaskForGroup(GROUP_OF_PETRA);
        final var foreignTask = createTask(
                "\"candidateGroups\": [ \"" + GROUP_OF_MARTIN + "\" ], \"assignee\": \"martin\"");

        final var response = guiPatch(cookieOfPetra, "/usertask/claim", """
                { "userTaskIds": [ "%s", "%s" ] }
                """.formatted(ownTask, foreignTask));
        assertThat(response.statusCode()).isEqualTo(200);

        assertThat(assigneeOf(cookieOfPetra, ownTask)).isEqualTo(USER_PETRA);
        assertThat(assigneeOf(cookieOfMartin, foreignTask)).isEqualTo(USER_MARTIN);

    }

    // rule 3: each of the three delivered lists answers by its own rule

    /**
     * The list of what is somebody's own holds no task which merely addresses one of their groups,
     * so its task page does not hold it either, while the main list holds both.
     */
    @Test
    void theListOfTheUsersOwnTasksAnswersByItsOwnNarrowerRule() {

        final var groupTask = createTaskForGroup(GROUP_OF_MARTIN);

        assertThat(tasksListedFor(cookieOfMartin, "")).containsExactly(groupTask);
        assertThat(tasksListedFor(cookieOfMartin, "/current-user")).isEmpty();
        assertThat(detailStatusFor(cookieOfMartin, "", groupTask)).isEqualTo(200);
        assertThat(detailStatusFor(cookieOfMartin, "/current-user", groupTask)).isEqualTo(404);

    }

    /**
     * The list of what a user's groups may take drops a task the moment that user claims it, and
     * its task page drops it with the list. The main list keeps the task either way, which is where
     * the user finds it afterwards.
     */
    @Test
    void theListOfWhatTheGroupsMayTakeDropsATaskOnceTheUserClaimsIt() {

        final var groupTask = createTaskForGroup(GROUP_OF_MARTIN);

        assertThat(tasksListedFor(cookieOfMartin, "/user-roles")).containsExactly(groupTask);
        assertThat(detailStatusFor(cookieOfMartin, "/user-roles", groupTask)).isEqualTo(200);

        assertThat(guiPatch(cookieOfMartin, "/user-roles/usertask/" + groupTask + "/claim", null)
                .statusCode()).isEqualTo(200);

        assertThat(tasksListedFor(cookieOfMartin, "/user-roles")).isEmpty();
        assertThat(detailStatusFor(cookieOfMartin, "/user-roles", groupTask)).isEqualTo(404);
        assertThat(detailStatusFor(cookieOfMartin, "", groupTask)).isEqualTo(200);
        assertThat(assigneeOf(cookieOfMartin, groupTask)).isEqualTo(USER_MARTIN);

    }

    /**
     * Giving a task back is the one action which takes the task out of the very list it was made
     * from, and it still has to be answered as done rather than as unknown.
     */
    @Test
    void givingBackATaskFromTheListOfOwnTasksIsAnsweredAsDone() {

        final var userTaskId = createTask(
                "\"candidateGroups\": [ \"" + GROUP_OF_MARTIN + "\" ], \"assignee\": \"martin\"");

        assertThat(tasksListedFor(cookieOfMartin, "/current-user")).containsExactly(userTaskId);

        assertThat(guiPatch(cookieOfMartin,
                "/current-user/usertask/" + userTaskId + "/claim?unclaim=true", null)
                .statusCode()).isEqualTo(200);

        // gone from the list it was given back in, still there for the group it belongs to
        assertThat(tasksListedFor(cookieOfMartin, "/current-user")).isEmpty();
        assertThat(tasksListedFor(cookieOfMartin, "")).containsExactly(userTaskId);

        final var task = guiGet(cookieOfMartin, "/usertask/" + userTaskId);
        assertThat(task.statusCode()).isEqualTo(200);
        assertThat(json(task).read("$.assignee", Object.class)).isNull();

    }

    // rule 4: an application of its own

    /**
     * The example controller keeps the tasks of a user's groups even when they name that user as
     * excluded, which no delivered list does. The list and the task page follow it together, which
     * is the point: the derived application described a visibility and got both.
     */
    @Test
    void anApplicationWithAVisibilityOfItsOwnGetsListAndTaskPageFromIt() {

        final var excludingTask = createTask(
                "\"candidateGroups\": [ \"" + GROUP_OF_MARTIN + "\" ], "
                        + "\"excludedCandidateUsers\": [ \"" + USER_MARTIN + "\" ]");

        assertThat(tasksListedFor(cookieOfMartin, "")).isEmpty();
        assertThat(detailStatusFor(cookieOfMartin, "", excludingTask)).isEqualTo(404);

        assertThat(tasksListedFor(cookieOfMartin, "/itest-group-wide"))
                .containsExactly(excludingTask);
        assertThat(detailStatusFor(cookieOfMartin, "/itest-group-wide", excludingTask))
                .isEqualTo(200);

    }

    /** The view of its own is a view, not a way around every other rule: petra is in no group of it. */
    @Test
    void aVisibilityOfItsOwnStillKeepsOutWhoItDoesNotName() {

        final var userTaskId = createTaskForGroup(GROUP_OF_MARTIN);

        assertThat(tasksListedFor(cookieOfPetra, "/itest-group-wide")).isEmpty();
        assertThat(detailStatusFor(cookieOfPetra, "/itest-group-wide", userTaskId)).isEqualTo(404);

    }

    // the workflow module a user is kept out of

    @Test
    void openingAWorkflowModuleHiddenFromTheUserIsAnsweredAsUnknown() {

        final var restrictedModuleId = unique("restricted-module");
        final var response = bpmsV1_1("/workflow-module/" + restrictedModuleId, """
                {
                  "id": "%s",
                  "uri": "%s",
                  "taskProviderApiUriPath": "/task-provider/v1",
                  "workflowProviderApiUriPath": "/workflow-provider/v1",
                  "accessibleToGroups": [ "%s" ]
                }
                """.formatted(restrictedModuleId, SOME_MODULE_URI, GROUP_OF_MARTIN));
        assertThat(response.statusCode()).isEqualTo(200);

        assertThat(guiGet(cookieOfMartin, "/workflow-module/" + restrictedModuleId).statusCode())
                .isEqualTo(200);
        assertThat(guiGet(cookieOfPetra, "/workflow-module/" + restrictedModuleId).statusCode())
                .isEqualTo(404);

    }

}
