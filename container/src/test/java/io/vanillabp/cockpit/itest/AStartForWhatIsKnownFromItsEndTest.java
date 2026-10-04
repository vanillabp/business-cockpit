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
 * A task or a case the cockpit knew from its end alone used to be stored without a start, and the
 * user interface needs one for every record. A changeset gives such a record its end as its start
 * and marks it as still waiting for its creation. This test seeds that shape of document and
 * drives the changeset against the running MongoDB, the way {@code LatestEventAtChangesetTest}
 * does. No request to the application could do it, because changesets are applied once while the
 * context starts.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class AStartForWhatIsKnownFromItsEndTest extends ItestBase {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-09-01T10:15:30Z");

    private static final OffsetDateTime ENDED_AT = OffsetDateTime.parse("2026-09-01T10:20:00Z");

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

    private String userTask(
            final String userTaskId,
            final OffsetDateTime timestamp) {

        return """
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
                """.formatted(unique("event"), userTaskId, timestamp, moduleId, token);

    }

    private String workflow(
            final String workflowId,
            final OffsetDateTime timestamp) {

        return """
                {
                  "id": "%s",
                  "workflowId": "%s",
                  "timestamp": "%s",
                  "initiator": "martin",
                  "workflowModuleId": "%s",
                  "bpmnProcessId": "taxi-ride",
                  "title": { "en": "Ride request 4711" },
                  "uiUriPath": "/remoteEntry.js",
                  "uiUriType": "WEBPACK_MF_REACT",
                  "detailsFulltextSearch": "%s"
                }
                """.formatted(unique("event"), workflowId, timestamp, moduleId, token);

    }

    /** Rewrites a stored record the way the cockpit stored an end which arrived alone before. */
    private void storedTheOldWay(
            final String id,
            final String collection) {

        mongo.updateFirst(
                new Query(Criteria.where("_id").is(id)),
                new Update().unset("createdAt").unset("knownFromItsEndAlone"),
                collection);
        assertThat(stored(id, collection).get("createdAt")).isNull();

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
    void aTaskKnownFromItsEndAloneStartsAtItsEndAndStillTakesItsCreation() {

        final var userTaskId = unique("task");
        assertThat(bpmsV1_1("/usertask/" + userTaskId + "/completed", userTask(userTaskId, ENDED_AT))
                .statusCode()).isEqualTo(200);
        storedTheOldWay(userTaskId, UserTask.COLLECTION_NAME);

        userTaskChangesets.startWhatIsKnownFromItsEndAlone(mongo);

        final var migrated = stored(userTaskId, UserTask.COLLECTION_NAME);
        assertThat(at(migrated, "createdAt").toInstant()).isEqualTo(ENDED_AT.toInstant());
        assertThat(migrated.getBoolean("knownFromItsEndAlone")).isTrue();

        assertThat(bpmsV1_1("/usertask/created", userTask(userTaskId, CREATED_AT)).statusCode())
                .isEqualTo(200);

        final var created = stored(userTaskId, UserTask.COLLECTION_NAME);
        assertThat(at(created, "createdAt").toInstant()).isEqualTo(CREATED_AT.toInstant());
        assertThat(at(created, "endedAt").toInstant()).isEqualTo(ENDED_AT.toInstant());
        assertThat(created.getBoolean("knownFromItsEndAlone")).isFalse();

    }

    @Test
    void aTaskWithAStartIsLeftAsItIs() {

        final var userTaskId = unique("task");
        assertThat(bpmsV1_1("/usertask/created", userTask(userTaskId, CREATED_AT)).statusCode())
                .isEqualTo(200);

        userTaskChangesets.startWhatIsKnownFromItsEndAlone(mongo);

        final var unchanged = stored(userTaskId, UserTask.COLLECTION_NAME);
        assertThat(at(unchanged, "createdAt").toInstant()).isEqualTo(CREATED_AT.toInstant());
        assertThat(unchanged.getBoolean("knownFromItsEndAlone")).isFalse();

    }

    @Test
    void aCaseKnownFromItsEndAloneStartsAtItsEnd() {

        final var workflowId = unique("workflow");
        assertThat(bpmsV1_1("/workflow/" + workflowId + "/completed", workflow(workflowId, ENDED_AT))
                .statusCode()).isEqualTo(200);
        storedTheOldWay(workflowId, Workflow.COLLECTION_NAME);

        workflowChangesets.startWhatIsKnownFromItsEndAlone(mongo);

        final var migrated = stored(workflowId, Workflow.COLLECTION_NAME);
        assertThat(at(migrated, "createdAt").toInstant()).isEqualTo(ENDED_AT.toInstant());
        assertThat(migrated.getBoolean("knownFromItsEndAlone")).isTrue();

    }

}
