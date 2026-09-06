package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The reference application shows a user the workflows addressed to them, and the rules it applies
 * are the ones pinned here. Each test names the rule it stands for, because the rules live in a
 * Mongo query (WorkflowlistService.buildWorkflowlistCriteria) plus the equivalent check the detail
 * view does, and reading either of them tells you the mechanics rather than the intent.
 *
 * <p>The rules:
 *
 * <ol>
 *   <li>A workflow which names neither users nor groups is open to everyone. Absent and empty are
 *       the same thing here, because the flag deciding it is derived from both lists being empty.
 *   <li>A workflow naming users or groups is shown to the users it names and to the members of the
 *       groups it names, and to nobody else.
 *   <li>The groups of a user are the authorities of the request, which the group hierarchies of all
 *       registered workflow modules have already widened. So a hierarchy reaches the workflow list
 *       the same way it reaches the task list.
 *   <li>The detail view answers by rules one to three as well and reports a workflow the user may
 *       not see as unknown.
 * </ol>
 */
class WorkflowlistPermissionsTest extends ItestBase {

    /** Registrations need a URI, but no test here follows the proxy route to it. */
    private static final String SOME_MODULE_URI = "http://localhost:65002";

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

    private String createWorkflow(
            final String accessFields) {

        final var workflowId = unique("workflow");
        final var response = bpmsV1_1("/workflow/created", """
                {
                  "id": "%s",
                  "workflowId": "%s",
                  "timestamp": "%s",
                  "workflowModuleId": "%s",
                  "bpmnProcessId": "taxi-ride",
                  "bpmnProcessVersion": "1",
                  "businessId": "ride-4711",
                  "initiator": "martin",
                  "title": { "en": "Ride request 4711" },
                  "uiUriPath": "/remoteEntry.js",
                  "uiUriType": "WEBPACK_MF_REACT",
                  "detailsFulltextSearch": "%s"%s
                }
                """.formatted(unique("event"), workflowId, isoNow(), moduleId, token,
                        accessFields.isEmpty() ? "" : ",\n  " + accessFields));
        assertThat(response.statusCode()).isEqualTo(200);
        return workflowId;

    }

    private String createWorkflowAccessibleToGroups(
            final String... groups) {

        return createWorkflow("\"accessibleToGroups\": [ " + asJsonStrings(groups) + " ]");

    }

    private String createWorkflowAccessibleToUsers(
            final String... users) {

        return createWorkflow("\"accessibleToUsers\": [ " + asJsonStrings(users) + " ]");

    }

    private static String asJsonStrings(
            final String... values) {

        return Arrays
                .stream(values)
                .map(value -> "\"" + value + "\"")
                .collect(Collectors.joining(", "));

    }

    @SuppressWarnings("unchecked")
    private List<String> workflowsListedFor(
            final String cookie) {

        return workflowList(cookie, token, "Active").read("$.workflows[*].id", List.class);

    }

    private int detailStatusFor(
            final String cookie,
            final String workflowId) {

        return guiGet(cookie, "/workflow/" + workflowId).statusCode();

    }

    /** One entry of the {@code groupHierarchy} array: members of {@code group} also get the targets. */
    private void registerModuleGranting(
            final String group,
            final String target) {

        final var response = bpmsV1_1("/workflow-module/" + moduleId, """
                {
                  "id": "%s",
                  "uri": "%s",
                  "taskProviderApiUriPath": "/task-provider/v1",
                  "workflowProviderApiUriPath": "/workflow-provider/v1",
                  "groupHierarchy": [ { "group": "%s", "targets": [ "%s" ] } ]
                }
                """.formatted(moduleId, SOME_MODULE_URI, group, target));
        assertThat(response.statusCode()).isEqualTo(200);

    }

    // rule 1: no access information means open to everyone

    @Test
    void aWorkflowWithoutAccessInformationIsListedForEveryUser() {

        final var workflowId = createWorkflow("");

        assertThat(workflowsListedFor(cookieOfMartin)).containsExactly(workflowId);
        assertThat(workflowsListedFor(cookieOfPetra)).containsExactly(workflowId);
        assertThat(detailStatusFor(cookieOfMartin, workflowId)).isEqualTo(200);
        assertThat(detailStatusFor(cookieOfPetra, workflowId)).isEqualTo(200);

    }

    /**
     * An empty list is the same as no list at all, since what decides the case is both lists being
     * empty rather than the reporting module having left them out.
     */
    @Test
    void aWorkflowWithEmptyAccessListsIsListedForEveryUser() {

        final var workflowId = createWorkflow(
                "\"accessibleToUsers\": [], \"accessibleToGroups\": []");

        assertThat(workflowsListedFor(cookieOfMartin)).containsExactly(workflowId);
        assertThat(workflowsListedFor(cookieOfPetra)).containsExactly(workflowId);

    }

    // rule 2: named users and groups, and nobody else

    @Test
    void aWorkflowIsListedOnlyForUsersOfItsAccessibleToGroups() {

        final var workflowId = createWorkflowAccessibleToGroups(GROUP_OF_MARTIN);

        assertThat(workflowsListedFor(cookieOfMartin)).containsExactly(workflowId);
        assertThat(workflowsListedFor(cookieOfPetra)).isEmpty();

    }

    @Test
    void aWorkflowIsListedOnlyForTheUsersOfItsAccessibleToUsers() {

        final var workflowId = createWorkflowAccessibleToUsers(USER_MARTIN);

        assertThat(workflowsListedFor(cookieOfMartin)).containsExactly(workflowId);
        assertThat(workflowsListedFor(cookieOfPetra)).isEmpty();

    }

    /**
     * The two lists widen each other instead of narrowing: petra is not in the group, yet she is
     * named individually, so both users see the workflow.
     */
    @Test
    void beingNamedAsUserIsEnoughWithoutBeingInTheGroup() {

        final var workflowId = createWorkflow(
                "\"accessibleToUsers\": [ \"" + USER_PETRA + "\" ], "
                        + "\"accessibleToGroups\": [ \"" + GROUP_OF_MARTIN + "\" ]");

        assertThat(workflowsListedFor(cookieOfMartin)).containsExactly(workflowId);
        assertThat(workflowsListedFor(cookieOfPetra)).containsExactly(workflowId);

    }

    /**
     * A workflow restricted to a group nobody of the two users belongs to is gone for both of them,
     * which rules out the reading that an unmatched restriction falls back to showing everything.
     */
    @Test
    void aWorkflowRestrictedToAThirdGroupIsListedForNobody() {

        createWorkflowAccessibleToGroups(unique("dispatchers"));

        assertThat(workflowsListedFor(cookieOfMartin)).isEmpty();
        assertThat(workflowsListedFor(cookieOfPetra)).isEmpty();

    }

    // rule 3: the group hierarchy of the registered modules counts

    /**
     * Martin is in accounting, the module grants the night-shift group to everybody in accounting,
     * so a workflow addressed to night-shift reaches him. His cookie predates the registration,
     * because the hierarchy is applied while the JWT of a request is read.
     */
    @Test
    void aGroupGrantedByTheHierarchyOpensTheWorkflowUp() {

        final var nightShift = unique("night-shift");
        final var workflowId = createWorkflowAccessibleToGroups(nightShift);
        assertThat(workflowsListedFor(cookieOfMartin)).isEmpty();

        registerModuleGranting(GROUP_OF_MARTIN, nightShift);

        await().untilAsserted(() -> {
            assertThat(workflowsListedFor(cookieOfMartin)).containsExactly(workflowId);
            assertThat(workflowsListedFor(cookieOfPetra)).isEmpty();
            assertThat(detailStatusFor(cookieOfMartin, workflowId)).isEqualTo(200);
        });

    }

    // rule 4: the detail view answers by the same rules

    @Test
    void theDetailViewOfAWorkflowHiddenFromTheListIsReportedAsUnknown() {

        final var workflowId = createWorkflowAccessibleToGroups(GROUP_OF_MARTIN);

        assertThat(detailStatusFor(cookieOfMartin, workflowId)).isEqualTo(200);
        assertThat(detailStatusFor(cookieOfPetra, workflowId)).isEqualTo(404);

    }

    /**
     * The same answer as for a workflow which was never reported, so that trying ids tells a user
     * nothing about workflows they are kept away from.
     */
    @Test
    void theDetailViewAnswersTheSameForAHiddenAndAnUnknownWorkflow() {

        final var hiddenWorkflowId = createWorkflowAccessibleToUsers(USER_MARTIN);

        final var hidden = guiGet(cookieOfPetra, "/workflow/" + hiddenWorkflowId);
        final var unknown = guiGet(cookieOfPetra, "/workflow/" + unique("never-reported"));
        assertThat(hidden.statusCode()).isEqualTo(unknown.statusCode());
        assertThat(hidden.body()).isEqualTo(unknown.body());

    }

    // the user tasks of a workflow, which have their own two modes

    private String createTaskOfWorkflow(
            final String workflowId,
            final String candidateGroup) {

        final var userTaskId = unique("task");
        final var response = bpmsV1_1("/usertask/created", """
                {
                  "id": "%s",
                  "userTaskId": "%s",
                  "timestamp": "%s",
                  "workflowModuleId": "%s",
                  "workflowId": "%s",
                  "bpmnProcessId": "taxi-ride",
                  "title": { "en": "Do ride" },
                  "taskDefinition": "do-ride",
                  "uiUriPath": "/remoteEntry.js",
                  "uiUriType": "WEBPACK_MF_REACT",
                  "candidateGroups": [ "%s" ]
                }
                """.formatted(unique("event"), userTaskId, isoNow(), moduleId, workflowId,
                        candidateGroup));
        assertThat(response.statusCode()).isEqualTo(200);
        return userTaskId;

    }

    @SuppressWarnings("unchecked")
    private List<String> userTasksOfWorkflow(
            final String cookie,
            final String workflowId,
            final boolean limitListAccordingToCurrentUsersPermissions) {

        final var response = guiPost(cookie,
                "/workflow/" + workflowId + "/usertasks?llatcup="
                        + limitListAccordingToCurrentUsersPermissions,
                """
                { "pageSize": 50, "mode": "OpenTasks" }
                """);
        assertThat(response.statusCode()).isEqualTo(200);
        return json(response).read("$[*].id", List.class);

    }

    /**
     * The status-site of a workflow decides per request which of the two it wants, so both modes
     * are exercised against the same pair of tasks: one addressed to martin's group, one to
     * petra's. Limited, the caller sees the task they could work on; unlimited, they see what the
     * workflow is up to as a whole.
     */
    @Test
    void theUserTasksOfAWorkflowAreLimitedToTheCurrentUserOnlyWhenAskedFor() {

        final var workflowId = createWorkflow("");
        final var taskOfMartin = createTaskOfWorkflow(workflowId, GROUP_OF_MARTIN);
        final var taskOfPetra = createTaskOfWorkflow(workflowId, GROUP_OF_PETRA);

        assertThat(userTasksOfWorkflow(cookieOfMartin, workflowId, true))
                .containsExactly(taskOfMartin);
        assertThat(userTasksOfWorkflow(cookieOfPetra, workflowId, true))
                .containsExactly(taskOfPetra);

        assertThat(userTasksOfWorkflow(cookieOfMartin, workflowId, false))
                .containsExactlyInAnyOrder(taskOfMartin, taskOfPetra);
        assertThat(userTasksOfWorkflow(cookieOfPetra, workflowId, false))
                .containsExactlyInAnyOrder(taskOfMartin, taskOfPetra);

    }

    /**
     * Rule 4 covers this route as well. Without it the unlimited mode would hand petra the tasks of
     * a workflow whose detail view answers 404 for her, and a task carries enough of the workflow
     * (its title and the module it belongs to) to make that a way around the detail view.
     */
    @Test
    void theUserTasksOfAWorkflowHiddenFromTheUserAreNotListedInEitherMode() {

        final var workflowId = createWorkflowAccessibleToGroups(GROUP_OF_MARTIN);
        final var taskOfPetra = createTaskOfWorkflow(workflowId, GROUP_OF_PETRA);

        assertThat(userTasksOfWorkflow(cookieOfPetra, workflowId, true)).isEmpty();
        assertThat(userTasksOfWorkflow(cookieOfPetra, workflowId, false)).isEmpty();
        assertThat(userTasksOfWorkflow(cookieOfMartin, workflowId, false))
                .containsExactly(taskOfPetra);

    }

}
