package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Subscribes to the server-sent-events stream the way the single-page app does, authenticated by
 * the JWT cookie. It checks the two things a client relies on, the confirmation ping shortly after
 * subscribing and an update event when a user task changes.
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

}
