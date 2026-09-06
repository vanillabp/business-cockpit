package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.changesets.V000001;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Tasks reported without an assignee before the ingress mappers were fixed are stored with an
 * assignee whose id is null, and a claim on such a task answers HTTP 500. A changeset cleans them
 * up on the next startup - this test seeds exactly that shape of document and drives the changeset
 * against the running MongoDB, which no request to the application could trigger: changesets are
 * applied once while the context starts.
 */
class AssigneeCleanupChangesetTest extends ItestBase {

    @Autowired
    private MongoTemplate mongo;

    @Autowired
    private V000001 userTaskChangesets;

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

    private String reportTaskWithoutAssignee(
            final String candidateGroups) {

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
                  "candidateGroups": [ %s ],
                  "detailsFulltextSearch": "%s"
                }
                """.formatted(unique("event"), userTaskId, isoNow(), moduleId, candidateGroups, token));
        assertThat(response.statusCode()).isEqualTo(200);
        return userTaskId;

    }

    /**
     * Rewrites a stored task the way the mappers did before the fix.
     */
    private void giveTheTaskAnAssigneeWithoutAUserId(
            final String userTaskId) {

        final var phantom = new Document();
        phantom.put("id", null);
        phantom.put("fulltext", null);
        phantom.put("sort", null);
        mongo.updateFirst(
                new Query(Criteria.where("_id").is(userTaskId)),
                new Update().set("assignee", phantom).set("dangling", Boolean.FALSE),
                UserTask.COLLECTION_NAME);

    }

    private Document storedTask(
            final String userTaskId) {
        return mongo.findById(userTaskId, Document.class, UserTask.COLLECTION_NAME);
    }

    @Test
    void cleanupDropsThePhantomAssigneeSoTheTaskCanBeClaimed() {

        final var userTaskId = reportTaskWithoutAssignee("\"" + GROUP_OF_MARTIN + "\"");
        giveTheTaskAnAssigneeWithoutAUserId(userTaskId);
        assertThat(storedTask(userTaskId).get("assignee")).isNotNull();

        userTaskChangesets.removeAssigneesHavingNoUserId(mongo);

        assertThat(storedTask(userTaskId).get("assignee")).isNull();
        assertThat(guiPatch(cookie, "/usertask/" + userTaskId + "/claim", null)
                .statusCode()).isEqualTo(200);
        assertThat(json(guiGet(cookie, "/usertask/" + userTaskId))
                .read("$.assignee.id", String.class)).isEqualTo(USER_MARTIN);

    }

    @Test
    void cleanupMarksATaskNobodyIsResponsibleForAsDangling() {

        final var userTaskId = reportTaskWithoutAssignee("");
        giveTheTaskAnAssigneeWithoutAUserId(userTaskId);
        assertThat(storedTask(userTaskId).get("dangling")).isEqualTo(Boolean.FALSE);

        userTaskChangesets.removeAssigneesHavingNoUserId(mongo);

        assertThat(storedTask(userTaskId).get("dangling")).isEqualTo(Boolean.TRUE);

    }

    @Test
    void cleanupKeepsAnAssigneeWhoIsAUser() {

        final var userTaskId = reportTaskWithoutAssignee("\"" + GROUP_OF_MARTIN + "\"");
        assertThat(guiPatch(cookie, "/usertask/" + userTaskId + "/claim", null)
                .statusCode()).isEqualTo(200);

        userTaskChangesets.removeAssigneesHavingNoUserId(mongo);

        assertThat(json(guiGet(cookie, "/usertask/" + userTaskId))
                .read("$.assignee.id", String.class)).isEqualTo(USER_MARTIN);

    }

}
