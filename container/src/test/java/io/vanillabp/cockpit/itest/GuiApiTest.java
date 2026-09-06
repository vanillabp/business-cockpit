package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises the GUI API the way the single-page app uses it: login via basic auth producing a JWT
 * cookie, then task list interactions like paging, sorting, mark-as-read, claim, assign and
 * follow-up dates.
 */
class GuiApiTest extends ItestBase {

    private String moduleId;
    private String token;
    private String cookie;

    @BeforeEach
    void registerModuleAndLogin() {
        moduleId = unique("ride-module");
        token = unique("token");
        registerWorkflowModule(moduleId, "http://localhost:65000");
        cookie = loginToGui(USER_MARTIN);
    }

    private String createTask(
            final String dueDate) {

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
                  "assignee": "martin",
                  "dueDate": "%s",
                  "detailsFulltextSearch": "%s"
                }
                """.formatted(unique("event"), userTaskId, isoNow(), moduleId, dueDate, token));
        assertThat(response.statusCode()).isEqualTo(200);
        return userTaskId;

    }

    @Test
    void appInfoIsServedWithoutAuthentication() {

        final var response = send(HttpRequest
                .newBuilder(URI.create(url("/gui/api/v1/app/info")))
                .GET()
                .build());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json(response).read("$.titleShort", String.class)).isEqualTo("VanillaBP-BC");

    }

    @Test
    void protectedEndpointsRejectUnauthenticatedRequests() {

        final var response = send(HttpRequest
                .newBuilder(URI.create(url("/gui/api/v1/usertask")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{ \"pageNumber\": 0, \"pageSize\": 10 }"))
                .build());
        assertThat(response.statusCode()).isEqualTo(401);

    }

    @Test
    void loginRejectsWrongPassword() {

        final var response = send(HttpRequest
                .newBuilder(URI.create(url("/gui/api/v1/app/current-user")))
                .header("Authorization", basicAuth(USER_MARTIN, "wrong-password"))
                .GET()
                .build());
        assertThat(response.statusCode()).isEqualTo(401);

    }

    @Test
    void loginIssuesJwtCookieAndCurrentUserAnswersWithUserDetails() {

        assertThat(cookie).startsWith("bc=");

        final var response = guiGet(cookie, "/app/current-user");
        assertThat(response.statusCode()).isEqualTo(200);
        final var user = json(response);
        assertThat(user.read("$.id", String.class)).isEqualTo(USER_MARTIN);
        assertThat(user.read("$.email", String.class)).isEqualTo("martin@example.com");
        assertThat(user.read("$.groups[*].id", List.class))
                .contains(GROUP_OF_MARTIN, "bc-users");

    }

    @Test
    void taskListSupportsPagingAndSorting() {

        final var baseDueDate = OffsetDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS);
        final var firstTask = createTask(baseDueDate.toString());
        final var secondTask = createTask(baseDueDate.plusHours(1).toString());
        final var thirdTask = createTask(baseDueDate.plusHours(2).toString());

        final var firstPage = json(guiPost(cookie, "/usertask", """
                {
                  "pageNumber": 0,
                  "pageSize": 2,
                  "searchQueries": [ { "query": "%s" } ],
                  "sortAscending": true,
                  "mode": "OpenTasks"
                }
                """.formatted(token)));
        assertThat(firstPage.read("$.page.totalElements", Integer.class)).isEqualTo(3);
        assertThat(firstPage.read("$.page.totalPages", Integer.class)).isEqualTo(2);
        assertThat(firstPage.read("$.page.number", Integer.class)).isEqualTo(0);
        assertThat(firstPage.read("$.userTasks[*].id", List.class))
                .containsExactly(firstTask, secondTask);

        final var secondPage = json(guiPost(cookie, "/usertask", """
                {
                  "pageNumber": 1,
                  "pageSize": 2,
                  "searchQueries": [ { "query": "%s" } ],
                  "sortAscending": true,
                  "mode": "OpenTasks"
                }
                """.formatted(token)));
        assertThat(secondPage.read("$.userTasks[*].id", List.class))
                .containsExactly(thirdTask);

        final var descending = json(guiPost(cookie, "/usertask", """
                {
                  "pageNumber": 0,
                  "pageSize": 3,
                  "searchQueries": [ { "query": "%s" } ],
                  "sortAscending": false,
                  "mode": "OpenTasks"
                }
                """.formatted(token)));
        assertThat(descending.read("$.userTasks[*].id", List.class))
                .containsExactly(thirdTask, secondTask, firstTask);

    }

    @Test
    void markAsReadIsReflectedInTaskDetailsAndCanBeUndone() {

        final var userTaskId = createTask(OffsetDateTime.now().plusDays(1).toString());

        assertThat(json(guiGet(cookie, "/usertask/" + userTaskId))
                .read("$.read", String.class)).isNull();

        assertThat(guiPatch(cookie, "/usertask/" + userTaskId + "/mark-as-read", null)
                .statusCode()).isEqualTo(200);
        assertThat(json(guiGet(cookie, "/usertask/" + userTaskId))
                .read("$.read", String.class)).isNotNull();

        assertThat(guiPatch(cookie, "/usertask/" + userTaskId + "/mark-as-read?unread=true", null)
                .statusCode()).isEqualTo(200);
        assertThat(json(guiGet(cookie, "/usertask/" + userTaskId))
                .read("$.read", String.class)).isNull();

    }

    @Test
    void claimTakesOverTheTaskAndUnclaimReleasesIt() {

        // reported with another user's assignment, so claiming means taking the task over
        final var userTaskId = unique("task");
        final var created = bpmsV1_1("/usertask/created", """
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
                  "assignee": "petra",
                  "candidateGroups": [ "%s" ],
                  "detailsFulltextSearch": "%s"
                }
                """.formatted(unique("event"), userTaskId, isoNow(), moduleId, GROUP_OF_MARTIN, token));
        assertThat(created.statusCode()).isEqualTo(200);

        assertThat(guiPatch(cookie, "/usertask/" + userTaskId + "/claim", null)
                .statusCode()).isEqualTo(200);
        assertThat(json(guiGet(cookie, "/usertask/" + userTaskId))
                .read("$.assignee.id", String.class)).isEqualTo(USER_MARTIN);

        assertThat(guiPatch(cookie, "/usertask/" + userTaskId + "/claim?unclaim=true", null)
                .statusCode()).isEqualTo(200);
        assertThat(json(guiGet(cookie, "/usertask/" + userTaskId))
                .read("$.assignee", Object.class)).isNull();

    }

    /**
     * Documents a pre-existing bug, not desired behavior: a task reported over the BPMS API
     * without an assignee is stored with an assignee object carrying a null id (the v1.1 mapper
     * maps the absent assignee through the person mapper instead of leaving it null). The GUI
     * hides that phantom assignee, but claiming such a task runs into a NullPointerException,
     * surfacing as HTTP 500. Once the mapping is fixed, this test should start failing and be
     * updated to the intended behavior: a successful claim.
     */
    @Test
    void claimingATaskReportedWithoutAssigneeCurrentlyFails() {

        final var userTaskId = unique("task");
        final var created = bpmsV1_1("/usertask/created", """
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
                """.formatted(unique("event"), userTaskId, isoNow(), moduleId, GROUP_OF_MARTIN, token));
        assertThat(created.statusCode()).isEqualTo(200);

        assertThat(guiPatch(cookie, "/usertask/" + userTaskId + "/claim", null)
                .statusCode()).isEqualTo(500);

    }

    @Test
    void assignAddsACandidateUserAndUnassignRemovesIt() {

        final var userTaskId = createTask(OffsetDateTime.now().plusDays(1).toString());

        assertThat(guiPatch(cookie,
                "/usertask/" + userTaskId + "/assign?userId=" + USER_PETRA, null)
                .statusCode()).isEqualTo(200);
        assertThat(json(guiGet(cookie, "/usertask/" + userTaskId))
                .read("$.candidateUsers[*].id", List.class)).contains(USER_PETRA);

        assertThat(guiPatch(cookie,
                "/usertask/" + userTaskId + "/assign?userId=" + USER_PETRA + "&unassign=true", null)
                .statusCode()).isEqualTo(200);
        assertThat(json(guiGet(cookie, "/usertask/" + userTaskId))
                .read("$.candidateUsers[*].id", List.class)).doesNotContain(USER_PETRA);

    }

    @Test
    void followUpDateCanBeSetAndCleared() {

        final var userTaskId = createTask(OffsetDateTime.now().plusDays(1).toString());
        // the service normalizes follow-up dates to whole minutes
        final var followUpDate = OffsetDateTime.now().plusDays(3).truncatedTo(ChronoUnit.MINUTES);

        final var setResponse = guiPatch(cookie,
                "/usertask/" + userTaskId + "/follow-up-date",
                "{ \"timestamp\": \"" + followUpDate + "\" }");
        assertThat(setResponse.statusCode()).isEqualTo(200);
        assertThat(OffsetDateTime.parse(json(guiGet(cookie, "/usertask/" + userTaskId))
                .read("$.followUpDate", String.class)).toInstant())
                .isEqualTo(followUpDate.toInstant());

        final var clearResponse = guiPatch(cookie,
                "/usertask/" + userTaskId + "/follow-up-date", "{ }");
        assertThat(clearResponse.statusCode()).isEqualTo(200);
        assertThat(json(guiGet(cookie, "/usertask/" + userTaskId))
                .read("$.followUpDate", String.class)).isNull();

    }

    @Test
    void unknownUserTaskDetailAnswers404() {

        assertThat(guiGet(cookie, "/usertask/" + unique("missing")).statusCode()).isEqualTo(404);

    }

    @Test
    void workflowModulesListContainsRegisteredModuleWithProxiedUri() {

        final var listResponse = guiGet(cookie, "/workflow-module");
        assertThat(listResponse.statusCode()).isEqualTo(200);
        final var moduleIds = json(listResponse).read("$.modules[*].id", List.class);
        assertThat(moduleIds).contains(moduleId);

        final var detailResponse = guiGet(cookie, "/workflow-module/" + moduleId);
        assertThat(detailResponse.statusCode()).isEqualTo(200);
        assertThat(json(detailResponse).read("$.uri", String.class))
                .isEqualTo("/wm/" + moduleId);

    }

    @Test
    void findUsersReturnsTheUsersKnownToTheCockpit() {

        final var response = guiPost(cookie, "/user?limit=10", "");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json(response).read("$.users[*].id", List.class))
                .containsExactlyInAnyOrder(USER_MARTIN, USER_PETRA);

    }

}
