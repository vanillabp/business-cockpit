package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Subscribes to the server-sent-events stream the way the single-page app does, authenticated by
 * the JWT cookie. It checks the things a client relies on: the confirmation ping shortly after
 * subscribing, an update event when a user task changes, and no event about a task the person may
 * not see.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class SseUpdatesTest extends ItestBase {

    private Queue<String> openEventStream(
            final String cookie) {

        final var request = HttpRequest
                .newBuilder(URI.create(url("/gui/api/v1/updates")))
                .header("Cookie", cookie)
                .header("Accept", "text/event-stream")
                .GET()
                .build();
        final var response = HTTP
                .sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .join();
        assertThat(response.statusCode()).isEqualTo(200);

        final var lines = new ConcurrentLinkedQueue<String>();
        final var reader = new Thread(() -> {
            try {
                response.body().forEach(lines::add);
            } catch (RuntimeException e) {
                // the stream never completes. The read ends when the JVM shuts down
            }
        }, "sse-reader");
        reader.setDaemon(true);
        reader.start();
        return lines;

    }

    @Test
    void subscriptionIsConfirmedByPingShortlyAfterConnecting() {

        final var lines = openEventStream(loginToGui(USER_MARTIN));

        await().untilAsserted(() -> assertThat(lines)
                .anyMatch(line -> line.startsWith("event:") && line.contains("ping")));

    }

    @Test
    void userTaskChangeIsPushedToSubscribedClients() {

        final var moduleId = unique("sse-module");
        registerWorkflowModule(moduleId, "http://localhost:65000");
        final var lines = openEventStream(loginToGui(USER_MARTIN));

        // subscribe first, then trigger the change the client should learn about
        await().untilAsserted(() -> assertThat(lines)
                .anyMatch(line -> line.contains("ping")));

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
                  "assignee": "martin"
                }
                """.formatted(unique("event"), userTaskId, isoNow(), moduleId));
        assertThat(created.statusCode()).isEqualTo(200);

        // the update is fed by a MongoDB change stream and delivered in batches, so allow
        // for the collecting interval plus change stream latency
        await().untilAsserted(() -> assertThat(lines)
                .anyMatch(line -> line.startsWith("data:") && line.contains(userTaskId)));

    }

    /**
     * Giving a task back is the change which used to get lost on the way. It is an {@code $unset}
     * in MongoDB and therefore an {@code update}, the one operation the change stream reports
     * without the changed document, while a newly reported task and a claim both carry theirs
     * along. {@code ChangeStreamNotificationsTest} counts the three writes next to each other; this
     * one follows the update all the way to the browser.
     */
    @Test
    void givingAUserTaskBackIsPushedToSubscribedClients() {

        final var moduleId = unique("sse-unclaim-module");
        registerWorkflowModule(moduleId, "http://localhost:65000");
        final var cookie = loginToGui(USER_MARTIN);
        final var lines = openEventStream(cookie);

        await().untilAsserted(() -> assertThat(lines)
                .anyMatch(line -> line.contains("ping")));

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
                  "assignee": "martin"
                }
                """.formatted(
                        unique("event"), userTaskId, isoNow(), moduleId, GROUP_OF_MARTIN));
        assertThat(created.statusCode()).isEqualTo(200);

        await().untilAsserted(() -> assertThat(eventsAbout(userTaskId, lines))
                .as("the newly reported task reaches the browser")
                .isEqualTo(1));

        assertThat(guiPatch(cookie, "/usertask/" + userTaskId + "/claim?unclaim=true", null)
                .statusCode())
                .isEqualTo(200);

        await().untilAsserted(() -> {
            assertThat(eventsAbout(userTaskId, lines))
                    .as("the task given back reaches the browser as a second event; stream so "
                            + "far: %s", lines)
                    .isEqualTo(2);
            assertThat(lines)
                    .anyMatch(line -> line.startsWith("data:") && line.contains("UPDATE"));
        });

    }

    /**
     * The task is offered to petra's group only. Martin's stream must not hear of it. Martin gets
     * a task of his own afterwards, and once that one has arrived, the first one would have arrived
     * before it: both go through the same ticks in the order they were reported.
     */
    @Test
    void aChangeOfAForeignTaskReachesNoOtherStream() {

        final var moduleId = unique("sse-foreign-module");
        registerWorkflowModule(moduleId, "http://localhost:65000");
        final var martin = openEventStream(loginToGui(USER_MARTIN));
        final var petra = openEventStream(loginToGui(USER_PETRA));
        await().untilAsserted(() -> {
            assertThat(martin).anyMatch(line -> line.contains("ping"));
            assertThat(petra).anyMatch(line -> line.contains("ping"));
        });

        final var petrasTask = unique("task");
        reportCreated(moduleId, petrasTask, """
                "candidateGroups": [ "%s" ]
                """.formatted(GROUP_OF_PETRA));
        await().untilAsserted(() -> assertThat(eventsAbout(petrasTask, petra)).isEqualTo(1));

        final var martinsTask = unique("task");
        reportCreated(moduleId, martinsTask, """
                "assignee": "%s"
                """.formatted(USER_MARTIN));
        await().untilAsserted(() -> assertThat(eventsAbout(martinsTask, martin)).isEqualTo(1));

        assertThat(eventsAbout(petrasTask, martin))
                .as("martin's stream so far: %s", martin)
                .isZero();
        assertThat(eventsAbout(martinsTask, petra))
                .as("petra's stream so far: %s", petra)
                .isZero();

    }

    /**
     * The workflow module reports martin's task again and names petra as its assignee. The report
     * which created the task decides who sees it, so the task stays martin's. His stream tells his
     * list about the change, and petra's stream hears nothing of it.
     */
    @Test
    void aChangeNamingSomebodyElseKeepsTheTaskWhereItWas() {

        final var moduleId = unique("sse-reassign-module");
        registerWorkflowModule(moduleId, "http://localhost:65000");
        final var martinsCookie = loginToGui(USER_MARTIN);
        final var petrasCookie = loginToGui(USER_PETRA);
        final var martin = openEventStream(martinsCookie);
        final var petra = openEventStream(petrasCookie);
        await().untilAsserted(() -> {
            assertThat(martin).anyMatch(line -> line.contains("ping"));
            assertThat(petra).anyMatch(line -> line.contains("ping"));
        });

        final var token = unique("reassign");
        final var userTaskId = unique("task");
        reportCreated(moduleId, userTaskId, """
                "assignee": "%s",
                "detailsFulltextSearch": "%s"
                """.formatted(USER_MARTIN, token));
        await().untilAsserted(() -> assertThat(eventsAbout(userTaskId, martin)).isEqualTo(1));
        assertThat(userTaskList(martinsCookie, token, "OpenTasks")
                .read("$.userTasks[*].id", List.class))
                .containsExactly(userTaskId);

        final var updated = bpmsV1_1("/usertask/" + userTaskId + "/updated", """
                {
                  "id": "%s",
                  "updated": true,
                  "userTaskId": "%s",
                  "timestamp": "%s",
                  "workflowModuleId": "%s",
                  "bpmnProcessId": "taxi-ride",
                  "title": { "en": "Do ride" },
                  "taskDefinition": "do-ride",
                  "uiUriPath": "/remoteEntry.js",
                  "uiUriType": "WEBPACK_MF_REACT",
                  "assignee": "%s",
                  "detailsFulltextSearch": "%s"
                }
                """.formatted(unique("event"), userTaskId, isoNow(), moduleId, USER_PETRA, token));
        assertThat(updated.statusCode()).isEqualTo(200);

        await().untilAsserted(() -> assertThat(eventsAbout(userTaskId, martin))
                .as("martin is told about the change; stream so far: %s", martin)
                .isEqualTo(2));
        // both streams go through the same ticks, so a word to petra would have arrived by now
        assertThat(eventsAbout(userTaskId, petra))
                .as("petra's stream so far: %s", petra)
                .isZero();
        assertThat(userTaskList(martinsCookie, token, "OpenTasks")
                .read("$.userTasks[*].id", List.class))
                .containsExactly(userTaskId);
        assertThat(userTaskList(petrasCookie, token, "OpenTasks")
                .read("$.userTasks[*].id", List.class))
                .isEmpty();

    }

    private void reportCreated(
            final String moduleId,
            final String userTaskId,
            final String addressedTo) {

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
                  %s
                }
                """.formatted(unique("event"), userTaskId, isoNow(), moduleId, addressedTo));
        assertThat(created.statusCode()).isEqualTo(200);

    }

    /**
     * How many update events the stream has delivered about one user task. The payload is written
     * as indented JSON, one {@code data:} line per property, so the id appears on a line of its own
     * and counting those lines counts the events.
     */
    private static long eventsAbout(
            final String userTaskId,
            final Queue<String> lines) {

        return lines
                .stream()
                .filter(line -> line.startsWith("data:") && line.contains(userTaskId))
                .count();

    }

}
