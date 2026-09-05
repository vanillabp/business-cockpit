package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises the BPMS API v1.1 workflow events over plain HTTP and verifies the outcome through
 * the GUI API's workflow list and detail endpoints.
 */
class BpmsApiWorkflowLifecycleTest extends ItestBase {

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

    private String workflowPayload(
            final String workflowId,
            final String timestamp,
            final String extraFields) {

        return """
                {
                  "id": "%s",
                  "workflowId": "%s",
                  "timestamp": "%s",
                  "workflowModuleId": "%s",
                  "bpmnProcessId": "taxi-ride",
                  "bpmnProcessVersion": "1",
                  "businessId": "ride-4711",
                  "initiator": "martin",
                  "title": { "de": "Fahrtanfrage 4711", "en": "Ride request 4711" },
                  "uiUriPath": "/remoteEntry.js",
                  "uiUriType": "WEBPACK_MF_REACT",
                  "detailsFulltextSearch": "%s"%s
                }
                """.formatted(unique("event"), workflowId, timestamp, moduleId, token,
                extraFields.isEmpty() ? "" : ",\n" + extraFields);

    }

    @Test
    void createdWorkflowShowsUpInWorkflowlistWithAllReportedFields() {

        final var workflowId = unique("workflow");
        final var createdAt = OffsetDateTime.parse("2026-09-01T09:00:00+02:00");

        final var created = bpmsV1_1("/workflow/created", workflowPayload(
                workflowId,
                createdAt.toString(),
                """
                "details": { "ride-request": "4711" }
                """));
        assertThat(created.statusCode()).isEqualTo(200);

        final var list = workflowList(cookie, token, "Active");
        assertThat(list.read("$.workflows[*].id", List.class)).containsExactly(workflowId);

        final var detailResponse = guiGet(cookie, "/workflow/" + workflowId);
        assertThat(detailResponse.statusCode()).isEqualTo(200);
        final var workflow = json(detailResponse);
        assertThat(workflow.read("$.id", String.class)).isEqualTo(workflowId);
        assertThat(workflow.read("$.title.en", String.class)).isEqualTo("Ride request 4711");
        assertThat(workflow.read("$.bpmnProcessId", String.class)).isEqualTo("taxi-ride");
        assertThat(workflow.read("$.businessId", String.class)).isEqualTo("ride-4711");
        assertThat(workflow.read("$.workflowModuleId", String.class)).isEqualTo(moduleId);
        assertThat(workflow.read("$.uiUri", String.class)).isEqualTo("/wm/" + moduleId + "/remoteEntry.js");
        assertThat(workflow.read("$.workflowModuleUri", String.class)).isEqualTo("/wm/" + moduleId);
        assertThat(OffsetDateTime.parse(workflow.read("$.createdAt", String.class)).toInstant())
                .isEqualTo(createdAt.toInstant());
        assertThat(workflow.read("$.details['ride-request']", String.class)).isEqualTo("4711");
        assertThat(workflow.read("$.endedAt", String.class)).isNull();

    }

    @Test
    void unknownWorkflowDetailAnswers404() {

        final var response = guiGet(cookie, "/workflow/" + unique("missing"));
        assertThat(response.statusCode()).isEqualTo(404);

    }

    @Test
    void updateEventChangesTheTitle() {

        final var workflowId = unique("workflow");
        bpmsV1_1("/workflow/created", workflowPayload(workflowId, isoNow(), ""));

        final var updated = bpmsV1_1("/workflow/" + workflowId + "/updated", workflowPayload(
                workflowId,
                isoNow(),
                "")
                .replace("Ride request 4711", "Ride request 4711 updated"));
        assertThat(updated.statusCode()).isEqualTo(200);

        final var workflow = json(guiGet(cookie, "/workflow/" + workflowId));
        assertThat(workflow.read("$.title.en", String.class)).isEqualTo("Ride request 4711 updated");

    }

    /**
     * Like for user tasks, an update event for an unknown workflow creates it. While the
     * application was reactive this fallback answered HTTP 500, because it re-read the already
     * consumed request body.
     */
    @Test
    void updateEventForUnknownWorkflowCreatesIt() {

        final var workflowId = unique("workflow");
        final var response = bpmsV1_1("/workflow/" + workflowId + "/updated",
                workflowPayload(workflowId, isoNow(), ""));
        assertThat(response.statusCode()).isEqualTo(200);

        final var detailResponse = guiGet(cookie, "/workflow/" + workflowId);
        assertThat(detailResponse.statusCode()).isEqualTo(200);
        assertThat(json(detailResponse).read("$.id", String.class)).isEqualTo(workflowId);

    }

    @Test
    void completedEventEndsTheWorkflow() {

        final var workflowId = unique("workflow");
        bpmsV1_1("/workflow/created", workflowPayload(workflowId, isoNow(), ""));

        final var completed = bpmsV1_1("/workflow/" + workflowId + "/completed",
                workflowPayload(workflowId, isoNow(), ""));
        assertThat(completed.statusCode()).isEqualTo(200);

        final var workflow = json(guiGet(cookie, "/workflow/" + workflowId));
        assertThat(workflow.read("$.endedAt", String.class)).isNotNull();
        assertThat(workflowList(cookie, token, "Inactive")
                .read("$.workflows[*].id", List.class)).containsExactly(workflowId);

    }

    @Test
    void cancelledEventEndsTheWorkflow() {

        final var workflowId = unique("workflow");
        bpmsV1_1("/workflow/created", workflowPayload(workflowId, isoNow(), ""));

        final var cancelled = bpmsV1_1("/workflow/" + workflowId + "/cancelled",
                workflowPayload(workflowId, isoNow(), """
                        "comment": "customer cancelled"
                        """));
        assertThat(cancelled.statusCode()).isEqualTo(200);

        final var workflow = json(guiGet(cookie, "/workflow/" + workflowId));
        assertThat(workflow.read("$.endedAt", String.class)).isNotNull();

    }

    @Test
    void userTasksOfAWorkflowAreListedTogether() {

        final var workflowId = unique("workflow");
        bpmsV1_1("/workflow/created", workflowPayload(workflowId, isoNow(), ""));

        final var firstTaskId = unique("task");
        final var secondTaskId = unique("task");
        for (final var taskId : List.of(firstTaskId, secondTaskId)) {
            final var response = bpmsV1_1("/usertask/created", """
                    {
                      "id": "%s",
                      "userTaskId": "%s",
                      "timestamp": "%s",
                      "workflowModuleId": "%s",
                      "workflowId": "%s",
                      "bpmnProcessId": "taxi-ride",
                      "title": { "en": "Task of ride 4711" },
                      "taskDefinition": "do-ride",
                      "uiUriPath": "/remoteEntry.js",
                      "uiUriType": "WEBPACK_MF_REACT",
                      "assignee": "martin",
                      "detailsFulltextSearch": "%s"
                    }
                    """.formatted(unique("event"), taskId, isoNow(), moduleId, workflowId, token));
            assertThat(response.statusCode()).isEqualTo(200);
        }

        final var response = guiPost(cookie,
                "/workflow/" + workflowId + "/usertasks?llatcup=false",
                """
                { "pageSize": 10, "mode": "OpenTasks" }
                """);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json(response).read("$[*].id", List.class))
                .containsExactlyInAnyOrder(firstTaskId, secondTaskId);

    }

}
