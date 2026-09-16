package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.time.OffsetDateTime;
import java.util.Date;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Tasks and cases stored before the cockpit weighed reports against each other have no
 * {@code latestEventAt}, and every report about them would be applied whatever its timestamp said. A
 * changeset copies {@code createdAt} into the new property on the next startup, and this test seeds
 * that shape of document and drives the changeset against the running MongoDB. No request to the
 * application could do it, because changesets are applied once while the context starts.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class LatestEventAtChangesetTest extends ItestBase {

    @Autowired
    private MongoTemplate mongo;

    @Autowired
    private io.vanillabp.cockpit.tasklist.model.changesets.V000001 userTaskChangesets;

    @Autowired
    private io.vanillabp.cockpit.workflowlist.model.changesets.V000001 workflowChangesets;

    private String moduleId;
    private String token;

    @BeforeEach
    void registerModule() {
        moduleId = unique("ride-module");
        token = unique("token");
        registerWorkflowModule(moduleId, "http://localhost:65000");
    }

    private String reportTask(
            final OffsetDateTime createdAt) {

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
                  "detailsFulltextSearch": "%s"
                }
                """.formatted(unique("event"), userTaskId, createdAt, moduleId, token));
        assertThat(response.statusCode()).isEqualTo(200);
        return userTaskId;

    }

    private String reportWorkflow(
            final OffsetDateTime createdAt) {

        final var workflowId = unique("workflow");
        final var response = bpmsV1_1("/workflow/created", """
                {
                  "id": "%s",
                  "workflowId": "%s",
                  "timestamp": "%s",
                  "workflowModuleId": "%s",
                  "bpmnProcessId": "taxi-ride",
                  "title": { "en": "A ride" },
                  "uiUriPath": "/remoteEntry.js",
                  "uiUriType": "WEBPACK_MF_REACT",
                  "detailsFulltextSearch": "%s"
                }
                """.formatted(unique("event"), workflowId, createdAt, moduleId, token));
        assertThat(response.statusCode()).isEqualTo(200);
        return workflowId;

    }

    /** Rewrites a stored record the way it looked before the property existed. */
    private void takeAwayLatestEventAt(
            final String id,
            final String collection) {

        mongo.updateFirst(
                new Query(Criteria.where("_id").is(id)),
                new Update().unset("latestEventAt"),
                collection);

    }

    private Document stored(
            final String id,
            final String collection) {
        return mongo.findById(id, Document.class, collection);
    }

    private static OffsetDateTime at(
            final Document document,
            final String property) {

        final var value = document.get(property);
        return value == null
                ? null
                : ((Date) value).toInstant().atOffset(OffsetDateTime.now().getOffset());

    }

    @Test
    void aTaskFromBeforeGetsTheTimestampOfTheEventWhichCreatedIt() {

        final var createdAt = OffsetDateTime.parse("2026-09-01T10:15:30Z");
        final var userTaskId = reportTask(createdAt);
        takeAwayLatestEventAt(userTaskId, UserTask.COLLECTION_NAME);
        assertThat(stored(userTaskId, UserTask.COLLECTION_NAME).get("latestEventAt")).isNull();

        userTaskChangesets.introduceLatestEventAt(mongo);

        assertThat(at(stored(userTaskId, UserTask.COLLECTION_NAME), "latestEventAt").toInstant())
                .isEqualTo(createdAt.toInstant());

    }

    @Test
    void aTaskWithoutACreationTimestampStaysWithout() {

        final var userTaskId = reportTask(OffsetDateTime.parse("2026-09-01T10:15:30Z"));
        takeAwayLatestEventAt(userTaskId, UserTask.COLLECTION_NAME);
        // this is what a task looks like which the cockpit knows from its end alone. Nothing says
        // when it began
        mongo.updateFirst(
                new Query(Criteria.where("_id").is(userTaskId)),
                new Update().unset("createdAt"),
                UserTask.COLLECTION_NAME);

        userTaskChangesets.introduceLatestEventAt(mongo);

        assertThat(stored(userTaskId, UserTask.COLLECTION_NAME).get("latestEventAt")).isNull();

    }

    @Test
    void aCaseFromBeforeGetsTheTimestampOfTheEventWhichStartedIt() {

        final var createdAt = OffsetDateTime.parse("2026-09-01T09:00:00Z");
        final var workflowId = reportWorkflow(createdAt);
        takeAwayLatestEventAt(workflowId, Workflow.COLLECTION_NAME);
        assertThat(stored(workflowId, Workflow.COLLECTION_NAME).get("latestEventAt")).isNull();

        workflowChangesets.introduceLatestEventAt(mongo);

        assertThat(at(stored(workflowId, Workflow.COLLECTION_NAME), "latestEventAt").toInstant())
                .isEqualTo(createdAt.toInstant());

    }

    @Test
    void aCaseWithoutACreationTimestampStaysWithout() {

        final var workflowId = reportWorkflow(OffsetDateTime.parse("2026-09-01T09:00:00Z"));
        takeAwayLatestEventAt(workflowId, Workflow.COLLECTION_NAME);
        mongo.updateFirst(
                new Query(Criteria.where("_id").is(workflowId)),
                new Update().unset("createdAt"),
                Workflow.COLLECTION_NAME);

        workflowChangesets.introduceLatestEventAt(mongo);

        assertThat(stored(workflowId, Workflow.COLLECTION_NAME).get("latestEventAt")).isNull();

    }

    /**
     * The backfilled value is what the weighing of reports reads, so the guard has to hold for a
     * record which came through the changeset just as it does for one the cockpit stored itself.
     */
    @Test
    void aReportOlderThanTheBackfilledValueChangesNothing() {

        final var createdAt = OffsetDateTime.parse("2026-09-01T10:15:30Z");
        final var userTaskId = reportTask(createdAt);
        takeAwayLatestEventAt(userTaskId, UserTask.COLLECTION_NAME);
        userTaskChangesets.introduceLatestEventAt(mongo);

        final var older = bpmsV1_1("/usertask/" + userTaskId + "/updated", """
                {
                  "id": "%s",
                  "updated": true,
                  "userTaskId": "%s",
                  "timestamp": "2026-09-01T09:00:00Z",
                  "workflowModuleId": "%s",
                  "bpmnProcessId": "taxi-ride",
                  "title": { "en": "Do ride the old way" },
                  "taskDefinition": "do-ride",
                  "uiUriPath": "/remoteEntry.js",
                  "uiUriType": "WEBPACK_MF_REACT",
                  "detailsFulltextSearch": "%s"
                }
                """.formatted(unique("event"), userTaskId, moduleId, token));
        assertThat(older.statusCode()).isEqualTo(200);

        final var cookie = loginToGui(USER_MARTIN);
        assertThat(json(guiGet(cookie, "/usertask/" + userTaskId)).read("$.title.en", String.class))
                .isEqualTo("Do ride");

    }

}
