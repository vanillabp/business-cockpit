package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises the BPMS API v1.1 user task events over plain HTTP and verifies the outcome through
 * the GUI API, the read side a real user would see.
 */
class BpmsApiUserTaskLifecycleTest extends ItestBase {

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

    private String userTaskCreatedPayload(
            final String userTaskId,
            final String timestamp,
            final String extraFields) {

        return """
                {
                  "id": "%s",
                  "userTaskId": "%s",
                  "timestamp": "%s",
                  "workflowModuleId": "%s",
                  "bpmnProcessId": "taxi-ride",
                  "bpmnProcessVersion": "1",
                  "businessId": "ride-4711",
                  "title": { "de": "Fahrt 4711 durchführen", "en": "Do ride 4711" },
                  "taskDefinition": "do-ride",
                  "taskDefinitionTitle": { "de": "Fahrt durchführen", "en": "Do ride" },
                  "uiUriPath": "/remoteEntry.js",
                  "uiUriType": "WEBPACK_MF_REACT",
                  "detailsFulltextSearch": "%s"%s
                }
                """.formatted(unique("event"), userTaskId, timestamp, moduleId, token,
                extraFields.isEmpty() ? "" : ",\n" + extraFields);

    }

    @Test
    void createdUserTaskShowsUpInTasklistWithAllReportedFields() {

        final var userTaskId = unique("task");
        final var createdAt = OffsetDateTime.parse("2026-09-01T10:15:30+02:00");
        final var dueDate = OffsetDateTime.parse("2026-09-10T12:00:00+02:00");

        final var created = bpmsV1_1("/usertask/created", userTaskCreatedPayload(
                userTaskId,
                createdAt.toString(),
                """
                "assignee": "martin",
                "candidateUsers": [ "petra" ],
                "candidateGroups": [ "accounting" ],
                "dueDate": "%s",
                "details": { "customer": "passenger A" }
                """.formatted(dueDate)));
        assertThat(created.statusCode()).isEqualTo(200);

        final var list = userTaskList(cookie, token, "OpenTasks");
        assertThat(list.read("$.userTasks[*].id", List.class)).containsExactly(userTaskId);
        assertThat(list.read("$.page.totalElements", Integer.class)).isEqualTo(1);

        final var detailResponse = guiGet(cookie, "/usertask/" + userTaskId);
        assertThat(detailResponse.statusCode()).isEqualTo(200);
        final var task = json(detailResponse);
        assertThat(task.read("$.id", String.class)).isEqualTo(userTaskId);
        assertThat(task.read("$.title.de", String.class)).isEqualTo("Fahrt 4711 durchführen");
        assertThat(task.read("$.title.en", String.class)).isEqualTo("Do ride 4711");
        assertThat(task.read("$.taskDefinition", String.class)).isEqualTo("do-ride");
        assertThat(task.read("$.taskDefinitionTitle.en", String.class)).isEqualTo("Do ride");
        assertThat(task.read("$.bpmnProcessId", String.class)).isEqualTo("taxi-ride");
        assertThat(task.read("$.businessId", String.class)).isEqualTo("ride-4711");
        assertThat(task.read("$.workflowModuleId", String.class)).isEqualTo(moduleId);
        assertThat(task.read("$.uiUri", String.class)).isEqualTo("/wm/" + moduleId + "/remoteEntry.js");
        assertThat(task.read("$.uiUriType", String.class)).isEqualTo("WEBPACK_MF_REACT");
        assertThat(task.read("$.workflowModuleUri", String.class)).isEqualTo("/wm/" + moduleId);
        assertThat(task.read("$.assignee.id", String.class)).isEqualTo("martin");
        assertThat(task.read("$.candidateUsers[*].id", List.class)).containsExactly("petra");
        assertThat(task.read("$.candidateGroups[*].id", List.class)).containsExactly("accounting");
        assertThat(OffsetDateTime.parse(task.read("$.createdAt", String.class)).toInstant())
                .isEqualTo(createdAt.toInstant());
        assertThat(OffsetDateTime.parse(task.read("$.dueDate", String.class)).toInstant())
                .isEqualTo(dueDate.toInstant());
        assertThat(task.read("$.details.customer", String.class)).isEqualTo("passenger A");
        assertThat(task.read("$.endedAt", String.class)).isNull();

    }

    @Test
    void updateEventChangesTitleAssigneeAndDueDate() {

        final var userTaskId = unique("task");
        bpmsV1_1("/usertask/created", userTaskCreatedPayload(userTaskId, isoNow(), """
                "assignee": "martin"
                """));

        final var newDueDate = OffsetDateTime.parse("2026-10-01T08:00:00Z");
        final var updated = bpmsV1_1("/usertask/" + userTaskId + "/updated", """
                {
                  "id": "%s",
                  "updated": true,
                  "userTaskId": "%s",
                  "timestamp": "%s",
                  "workflowModuleId": "%s",
                  "bpmnProcessId": "taxi-ride",
                  "title": { "de": "Fahrt 4711 prüfen", "en": "Check ride 4711" },
                  "taskDefinition": "do-ride",
                  "uiUriPath": "/remoteEntry.js",
                  "uiUriType": "WEBPACK_MF_REACT",
                  "assignee": "petra",
                  "dueDate": "%s",
                  "detailsFulltextSearch": "%s"
                }
                """.formatted(unique("event"), userTaskId, isoNow(), moduleId, newDueDate, token));
        assertThat(updated.statusCode()).isEqualTo(200);

        final var task = json(guiGet(cookie, "/usertask/" + userTaskId));
        assertThat(task.read("$.title.en", String.class)).isEqualTo("Check ride 4711");
        assertThat(task.read("$.assignee.id", String.class)).isEqualTo("petra");
        assertThat(OffsetDateTime.parse(task.read("$.dueDate", String.class)).toInstant())
                .isEqualTo(newDueDate.toInstant());

    }

    /**
     * An update event for a task the cockpit never saw creates it, so a cockpit added to a running
     * system does not stay blind to the tasks that existed before. While the application was
     * reactive this fallback answered HTTP 500, because it re-read the already consumed request
     * body; reading the body once into an object removed the problem.
     */
    @Test
    void updateEventForUnknownTaskCreatesIt() {

        final var userTaskId = unique("task");
        final var response = bpmsV1_1("/usertask/" + userTaskId + "/updated",
                userTaskCreatedPayload(userTaskId, isoNow(), """
                        "assignee": "martin"
                        """));
        assertThat(response.statusCode()).isEqualTo(200);

        final var detailResponse = guiGet(cookie, "/usertask/" + userTaskId);
        assertThat(detailResponse.statusCode()).isEqualTo(200);
        assertThat(json(detailResponse).read("$.assignee.id", String.class)).isEqualTo("martin");

    }

    @Test
    void completedEventEndsTheTaskAndMovesItToClosedTasks() {

        final var userTaskId = unique("task");
        bpmsV1_1("/usertask/created", userTaskCreatedPayload(userTaskId, isoNow(), """
                "assignee": "martin"
                """));

        final var endedAt = isoNow();
        final var completed = bpmsV1_1("/usertask/" + userTaskId + "/completed", """
                {
                  "id": "%s",
                  "userTaskId": "%s",
                  "timestamp": "%s",
                  "initiator": "martin",
                  "workflowModuleId": "%s",
                  "bpmnProcessId": "taxi-ride",
                  "title": { "en": "Do ride 4711" },
                  "taskDefinition": "do-ride",
                  "uiUriPath": "/remoteEntry.js",
                  "uiUriType": "WEBPACK_MF_REACT",
                  "detailsFulltextSearch": "%s"
                }
                """.formatted(unique("event"), userTaskId, endedAt, moduleId, token));
        assertThat(completed.statusCode()).isEqualTo(200);

        final var task = json(guiGet(cookie, "/usertask/" + userTaskId));
        assertThat(task.read("$.endedAt", String.class)).isNotNull();

        assertThat(userTaskList(cookie, token, "OpenTasks")
                .read("$.userTasks[*].id", List.class)).isEmpty();
        assertThat(userTaskList(cookie, token, "ClosedTasksOnly")
                .read("$.userTasks[*].id", List.class)).containsExactly(userTaskId);

    }

    @Test
    void cancelledEventEndsTheTaskWithComment() {

        final var userTaskId = unique("task");
        bpmsV1_1("/usertask/created", userTaskCreatedPayload(userTaskId, isoNow(), """
                "assignee": "martin"
                """));

        final var cancelled = bpmsV1_1("/usertask/" + userTaskId + "/cancelled", """
                {
                  "id": "%s",
                  "userTaskId": "%s",
                  "timestamp": "%s",
                  "comment": "ride withdrawn",
                  "workflowModuleId": "%s",
                  "bpmnProcessId": "taxi-ride",
                  "title": { "en": "Do ride 4711" },
                  "taskDefinition": "do-ride",
                  "uiUriPath": "/remoteEntry.js",
                  "uiUriType": "WEBPACK_MF_REACT",
                  "detailsFulltextSearch": "%s"
                }
                """.formatted(unique("event"), userTaskId, isoNow(), moduleId, token));
        assertThat(cancelled.statusCode()).isEqualTo(200);

        final var task = json(guiGet(cookie, "/usertask/" + userTaskId));
        assertThat(task.read("$.endedAt", String.class)).isNotNull();
        assertThat(userTaskList(cookie, token, "OpenTasks")
                .read("$.userTasks[*].id", List.class)).isEmpty();

    }

    /**
     * The v1.1 API declares suspend/activate endpoints, but the container does not implement them:
     * the generated fallback answers 501. This test documents the current behavior so the
     * migration cannot silently change it.
     */
    @Test
    void suspendAndActivateAreNotImplemented() {

        final var userTaskId = unique("task");
        bpmsV1_1("/usertask/created", userTaskCreatedPayload(userTaskId, isoNow(), """
                "assignee": "martin"
                """));

        final var lifecycleEvent = """
                {
                  "id": "%s",
                  "userTaskId": "%s",
                  "timestamp": "%s"
                }
                """.formatted(unique("event"), userTaskId, isoNow());

        assertThat(bpmsV1_1("/usertask/" + userTaskId + "/suspended", lifecycleEvent).statusCode())
                .isEqualTo(501);
        assertThat(bpmsV1_1("/usertask/" + userTaskId + "/activated", lifecycleEvent).statusCode())
                .isEqualTo(501);

    }

    @Test
    void bpmsApiRejectsMissingAndWrongCredentials() {

        final var payload = userTaskCreatedPayload(unique("task"), isoNow(), "");

        final var withoutAuth = send(HttpRequest
                .newBuilder(URI.create(url("/bpms/api/v1_1/usertask/created")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build());
        assertThat(withoutAuth.statusCode()).isEqualTo(401);

        final var wrongPassword = send(HttpRequest
                .newBuilder(URI.create(url("/bpms/api/v1_1/usertask/created")))
                .header("Authorization", basicAuth(BPMS_API_USER, "wrong"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build());
        assertThat(wrongPassword.statusCode()).isEqualTo(401);

    }

}
