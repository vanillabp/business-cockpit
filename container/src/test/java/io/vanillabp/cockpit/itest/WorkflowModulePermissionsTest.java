package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The permission data a workflow module hands over when it registers - the groups it is accessible
 * to and the group hierarchy it contributes - has to take effect the moment the module registers
 * again, because a module announces changed permissions by re-registering and nobody restarts the
 * cockpit for that.
 *
 * <p>Both users log in once at the start of each test and keep their JWT cookie across the
 * re-registration, so a test that turns green afterwards proves the change reached the running
 * application, not just a fresh login.
 */
class WorkflowModulePermissionsTest extends ItestBase {

    /** The one group both users of the stub share, which makes a task visible to either of them. */
    private static final String GROUP_OF_BOTH = "bc-users";

    /** Registrations need a URI, but no test here follows the proxy route to it. */
    private static final String SOME_MODULE_URI = "http://localhost:65001";

    private String cookieOfMartin;
    private String cookieOfPetra;

    @BeforeEach
    void logInBothUsers() {

        cookieOfMartin = loginToGui(USER_MARTIN);
        cookieOfPetra = loginToGui(USER_PETRA);

    }

    private void register(
            final String workflowModuleId,
            final String permissionMembers) {

        final var response = bpmsV1_1(
                "/workflow-module/" + workflowModuleId,
                """
                {
                  "id": "%s",
                  "uri": "%s",
                  "taskProviderApiUriPath": "/task-provider/v1",
                  "workflowProviderApiUriPath": "/workflow-provider/v1"%s
                }
                """.formatted(workflowModuleId, SOME_MODULE_URI, permissionMembers));
        assertThat(response.statusCode()).isEqualTo(200);

    }

    private void registerWithoutPermissions(
            final String workflowModuleId) {

        register(workflowModuleId, "");

    }

    private void registerAccessibleToGroups(
            final String workflowModuleId,
            final String... groups) {

        register(workflowModuleId,
                ",\n  \"accessibleToGroups\": [ " + asJsonStrings(groups) + " ]");

    }

    private void registerWithGroupHierarchy(
            final String workflowModuleId,
            final String... hierarchyEntries) {

        register(workflowModuleId,
                ",\n  \"groupHierarchy\": [ " + String.join(", ", hierarchyEntries) + " ]");

    }

    /** One entry of the {@code groupHierarchy} array: members of {@code group} also get the targets. */
    private static String granting(
            final String group,
            final String... targets) {

        return "{ \"group\": \"" + group + "\", \"targets\": [ " + asJsonStrings(targets) + " ] }";

    }

    private static String asJsonStrings(
            final String... values) {

        return Arrays
                .stream(values)
                .map(value -> "\"" + value + "\"")
                .collect(Collectors.joining(", "));

    }

    @SuppressWarnings("unchecked")
    private List<String> modulesListedFor(
            final String cookie) {

        final var response = guiGet(cookie, "/workflow-module");
        assertThat(response.statusCode()).isEqualTo(200);
        return json(response).read("$.modules[*].id", List.class);

    }

    @SuppressWarnings("unchecked")
    private List<String> tasksListedFor(
            final String cookie,
            final String fulltextToken) {

        return userTaskList(cookie, fulltextToken, "OpenTasks")
                .read("$.userTasks[*].id", List.class);

    }

    /**
     * Reports a task addressed to a group only - no assignee, no candidate users - so that nothing
     * but group membership can make it show up in somebody's task list.
     */
    private String createTaskForGroup(
            final String workflowModuleId,
            final String fulltextToken,
            final String candidateGroup) {

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
                  "candidateGroups": [ "%s" ],
                  "detailsFulltextSearch": "%s"
                }
                """.formatted(
                        unique("event"), userTaskId, isoNow(), workflowModuleId, candidateGroup,
                        fulltextToken));
        assertThat(response.statusCode()).isEqualTo(200);
        return userTaskId;

    }

    // accessibleToGroups

    /**
     * Asserts the open half of the rule the module list is built on: a module is listed when its
     * accessibleToGroups holds one of the requesting user's groups, when the list is empty, or -
     * as here - when the module registered without one at all.
     */
    @Test
    void aModuleRegisteredWithoutAccessibleToGroupsIsListedForEveryUser() {

        final var moduleId = unique("open-module");
        registerWithoutPermissions(moduleId);

        await().untilAsserted(() -> {
            assertThat(modulesListedFor(cookieOfMartin)).contains(moduleId);
            assertThat(modulesListedFor(cookieOfPetra)).contains(moduleId);
        });

    }

    /**
     * The restricting half of the same rule: with a group listed, only members of that group find
     * the module in their list.
     */
    @Test
    void aModuleIsListedOnlyForUsersOfItsAccessibleToGroups() {

        final var moduleId = unique("restricted-module");
        registerAccessibleToGroups(moduleId, GROUP_OF_MARTIN);

        await().untilAsserted(() -> {
            assertThat(modulesListedFor(cookieOfMartin)).contains(moduleId);
            assertThat(modulesListedFor(cookieOfPetra)).doesNotContain(moduleId);
        });

    }

    /**
     * The module list is answered from the stored module documents on every request, so handing
     * over another group on re-registration moves the module between the two users right away.
     */
    @Test
    void reRegistrationWithOtherAccessibleToGroupsMovesTheModuleToTheOtherUser() {

        final var moduleId = unique("handed-over-module");
        registerAccessibleToGroups(moduleId, GROUP_OF_MARTIN);
        await().untilAsserted(() -> {
            assertThat(modulesListedFor(cookieOfMartin)).contains(moduleId);
            assertThat(modulesListedFor(cookieOfPetra)).doesNotContain(moduleId);
        });

        registerAccessibleToGroups(moduleId, GROUP_OF_PETRA);

        await().untilAsserted(() -> {
            assertThat(modulesListedFor(cookieOfPetra)).contains(moduleId);
            assertThat(modulesListedFor(cookieOfMartin)).doesNotContain(moduleId);
        });

    }

    /**
     * Dropping the list again opens the module up for everybody, the same as never having sent one.
     */
    @Test
    void reRegistrationWithoutAccessibleToGroupsOpensTheModuleUpAgain() {

        final var moduleId = unique("re-opened-module");
        registerAccessibleToGroups(moduleId, GROUP_OF_MARTIN);
        await().untilAsserted(() ->
                assertThat(modulesListedFor(cookieOfPetra)).doesNotContain(moduleId));

        registerWithoutPermissions(moduleId);

        await().untilAsserted(() ->
                assertThat(modulesListedFor(cookieOfPetra)).contains(moduleId));

    }

    /**
     * Names the reach of accessibleToGroups, which is narrower than the name suggests: it decides
     * the module list and nothing else. A user task is visible by its own assignee and candidates,
     * so a user who is kept out of the module still finds that module's tasks in their task list.
     */
    @Test
    void accessibleToGroupsHidesTheModuleButNotItsTasks() {

        final var moduleId = unique("hidden-module");
        final var token = unique("token");
        registerAccessibleToGroups(moduleId, GROUP_OF_MARTIN);
        final var userTaskId = createTaskForGroup(moduleId, token, GROUP_OF_BOTH);

        await().untilAsserted(() -> {
            assertThat(modulesListedFor(cookieOfPetra)).doesNotContain(moduleId);
            assertThat(tasksListedFor(cookieOfPetra, token)).containsExactly(userTaskId);
        });

    }

    // groupHierarchy

    /**
     * A hierarchy entry maps a group to the groups its members get on top, and the resolution is
     * transitive: martin is in accounting, accounting grants the dispatcher group and that one
     * grants the night-shift group, so a task addressed to night-shift reaches him.
     */
    @Test
    void aGroupHierarchyGrantsItsTargetGroupsTransitively() {

        final var moduleId = unique("hierarchy-module");
        final var dispatchers = unique("dispatchers");
        final var nightShift = unique("night-shift");
        final var token = unique("token");
        registerWithGroupHierarchy(moduleId,
                granting(GROUP_OF_MARTIN, dispatchers),
                granting(dispatchers, nightShift));
        final var userTaskId = createTaskForGroup(moduleId, token, nightShift);

        await().untilAsserted(() -> {
            assertThat(tasksListedFor(cookieOfMartin, token)).containsExactly(userTaskId);
            assertThat(tasksListedFor(cookieOfPetra, token)).isEmpty();
        });

    }

    /**
     * The hierarchies of all registered modules are merged into one, and the merged result is
     * applied while the JWT of a request is read. So a re-registration reaches the next request of
     * a user who is already logged in - the cookie both users hold here predates it.
     */
    @Test
    void reRegistrationWithAChangedHierarchyMovesTheTaskToTheOtherUser() {

        final var moduleId = unique("re-hierarchized-module");
        final var nightShift = unique("night-shift");
        final var token = unique("token");
        registerWithGroupHierarchy(moduleId, granting(GROUP_OF_MARTIN, nightShift));
        final var userTaskId = createTaskForGroup(moduleId, token, nightShift);
        await().untilAsserted(() -> {
            assertThat(tasksListedFor(cookieOfMartin, token)).containsExactly(userTaskId);
            assertThat(tasksListedFor(cookieOfPetra, token)).isEmpty();
        });

        registerWithGroupHierarchy(moduleId, granting(GROUP_OF_PETRA, nightShift));

        await().untilAsserted(() -> {
            assertThat(tasksListedFor(cookieOfPetra, token)).containsExactly(userTaskId);
            assertThat(tasksListedFor(cookieOfMartin, token)).isEmpty();
        });

    }

    /**
     * Registering without a hierarchy revokes what the module granted before, which is how a module
     * that dropped its hierarchy from its configuration takes the extra groups away again.
     */
    @Test
    void reRegistrationWithoutAHierarchyRevokesTheGrantedGroups() {

        final var moduleId = unique("de-hierarchized-module");
        final var nightShift = unique("night-shift");
        final var token = unique("token");
        registerWithGroupHierarchy(moduleId, granting(GROUP_OF_MARTIN, nightShift));
        final var userTaskId = createTaskForGroup(moduleId, token, nightShift);
        await().untilAsserted(() ->
                assertThat(tasksListedFor(cookieOfMartin, token)).containsExactly(userTaskId));

        registerWithoutPermissions(moduleId);

        await().untilAsserted(() ->
                assertThat(tasksListedFor(cookieOfMartin, token)).isEmpty());

    }

    /**
     * Because the hierarchies are merged across modules instead of being kept per module, a group
     * one module grants counts everywhere: it opens up another module whose accessibleToGroups
     * names it, although that module knows nothing about the hierarchy.
     */
    @Test
    void aHierarchyOfOneModuleWidensAccessToAnother() {

        final var grantingModuleId = unique("granting-module");
        final var guardedModuleId = unique("guarded-module");
        final var supervisors = unique("supervisors");
        registerAccessibleToGroups(guardedModuleId, supervisors);
        await().untilAsserted(() ->
                assertThat(modulesListedFor(cookieOfMartin)).doesNotContain(guardedModuleId));

        registerWithGroupHierarchy(grantingModuleId, granting(GROUP_OF_MARTIN, supervisors));

        await().untilAsserted(() -> {
            assertThat(modulesListedFor(cookieOfMartin)).contains(guardedModuleId);
            assertThat(modulesListedFor(cookieOfPetra)).doesNotContain(guardedModuleId);
        });

    }

}
