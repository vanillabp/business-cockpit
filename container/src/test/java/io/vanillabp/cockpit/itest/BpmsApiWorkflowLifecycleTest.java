package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Exercises the BPMS API v1.1 workflow events over plain HTTP and verifies the outcome through
 * the GUI API's workflow list and detail endpoints.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
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

    /**
     * A case which is followed in another application is not served through the cockpit's proxy, so
     * the address the workflow module reported is what the user interface is told to open.
     */
    @Test
    void anExternalWorkflowKeepsTheAddressItWasReportedWith() {

        final var workflowId = unique("workflow");
        final var created = bpmsV1_1("/workflow/created", """
                {
                  "id": "%s",
                  "workflowId": "%s",
                  "timestamp": "%s",
                  "workflowModuleId": "%s",
                  "bpmnProcessId": "taxi-ride",
                  "bpmnProcessVersion": "1",
                  "initiator": "martin",
                  "title": { "en": "Ride request 4711" },
                  "uiUriPath": "https://orders.example.com/order/4711",
                  "uiUriType": "EXTERNAL",
                  "detailsFulltextSearch": "%s"
                }
                """.formatted(unique("event"), workflowId, isoNow(), moduleId, token));
        assertThat(created.statusCode()).isEqualTo(200);

        final var workflow = json(guiGet(cookie, "/workflow/" + workflowId));
        assertThat(workflow.read("$.uiUriType", String.class)).isEqualTo("EXTERNAL");
        assertThat(workflow.read("$.uiUri", String.class))
                .isEqualTo("https://orders.example.com/order/4711");
        assertThat(workflow.read("$.workflowModuleUri", String.class)).isEqualTo("/wm/" + moduleId);

    }

    /**
     * The outbox of a workflow module gives its entries no order, so the end of a case can reach the
     * cockpit before the report that the case was started. The cockpit used to answer 200 and store
     * nothing, and the creation arriving afterwards then left a case which was running for good.
     */
    @Test
    void completionArrivingBeforeTheCreationLeavesAFinishedCase() {

        final var workflowId = unique("workflow");
        final var createdAt = OffsetDateTime.parse("2026-09-01T09:00:00Z");
        final var endedAt = OffsetDateTime.parse("2026-09-01T09:30:00Z");

        assertThat(bpmsV1_1("/workflow/" + workflowId + "/completed",
                workflowPayload(workflowId, endedAt.toString(), "")).statusCode()).isEqualTo(200);
        assertThat(bpmsV1_1("/workflow/created", workflowPayload(
                workflowId,
                createdAt.toString(),
                """
                "details": { "ride-request": "4711" }
                """)).statusCode()).isEqualTo(200);

        assertThat(workflowList(cookie, token, "Active")
                .read("$.workflows[*].id", List.class)).isEmpty();

        // and the creation filled in what the completion could not report
        final var workflow = json(guiGet(cookie, "/workflow/" + workflowId));
        assertThat(OffsetDateTime.parse(workflow.read("$.endedAt", String.class)).toInstant())
                .isEqualTo(endedAt.toInstant());
        assertThat(OffsetDateTime.parse(workflow.read("$.createdAt", String.class)).toInstant())
                .isEqualTo(createdAt.toInstant());
        assertThat(workflow.read("$.details['ride-request']", String.class)).isEqualTo("4711");

    }

    /**
     * Two changes of one case can reach the cockpit the other way round. The timestamp of the event
     * decides which of them the cockpit keeps, not the moment it arrived.
     */
    @Test
    void anUpdateOlderThanTheStoredStateChangesNothing() {

        final var workflowId = unique("workflow");
        bpmsV1_1("/workflow/created", workflowPayload(
                workflowId, OffsetDateTime.parse("2026-09-01T09:00:00Z").toString(), ""));

        assertThat(bpmsV1_1("/workflow/" + workflowId + "/updated",
                workflowPayload(workflowId, "2026-09-01T11:00:00Z", "")
                        .replace("Ride request 4711", "Ride request 4711 as agreed"))
                .statusCode()).isEqualTo(200);

        assertThat(bpmsV1_1("/workflow/" + workflowId + "/updated",
                workflowPayload(workflowId, "2026-09-01T10:00:00Z", "")
                        .replace("Ride request 4711", "Ride request 4711 the old way"))
                .statusCode()).isEqualTo(200);

        final var workflow = json(guiGet(cookie, "/workflow/" + workflowId));
        assertThat(workflow.read("$.title.en", String.class)).isEqualTo("Ride request 4711 as agreed");

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
     * An update event for an unknown workflow creates it, the same way as for user tasks. While the
     * application was reactive this fallback answered HTTP 500, because it read the already
     * consumed request body a second time.
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
