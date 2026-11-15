package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowRepository;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import tools.jackson.databind.json.JsonMapper;

/**
 * A test of a workflow module asks the simulator again and again whether a workflow has ended.
 * Such a read must not write anything. If it did, a read which started before the report of the
 * end and finished after it would store the workflow as it was before, and the end would be gone.
 *
 * <p>Each round reports one workflow, then reports its end while other threads read it through
 * the official API. Afterwards the workflow must show its end. One round does not hit the gap
 * every time, so the test runs many of them. The reports and the reads go over HTTP, like they
 * do in a real test of a workflow module.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:a-reader-does-not-undo-the-end-of-a-workflow",
    // the simulator does not start without a user to log in as
    "dev-shell-simulator.users[0].id=john"
})
class AReaderDoesNotUndoTheEndOfAWorkflowTest {

  private static final int ROUNDS = 60;

  private static final int READERS = 2;

  private final HttpClient client = HttpClient.newHttpClient();

  @Value("${local.server.port}")
  private int port;

  @Autowired
  private WorkflowRepository workflows;

  @Autowired
  private JsonMapper jsonMapper;

  @AfterEach
  void removeTheWorkflows() {

    workflows.deleteAll();

  }

  private static String report(
      final int round,
      final String timestamp) {

    return """
        {
          "id": "event-%d-%s",
          "workflowId": "workflow-%d",
          "businessId": "business-%d",
          "timestamp": "%s",
          "bpmnProcessId": "loan_approval",
          "bpmnProcessVersion": "1",
          "workflowModuleId": "loan-approval",
          "uiUriPath": "/ui",
          "uiUriType": "WEBCOMPONENT",
          "title": { "en": "Approve a loan" },
          "initiator": "john",
          "details": { "status": "running" },
          "accessibleToUsers": [ "john" ],
          "accessibleToGroups": [ "ADMIN" ]
        }
        """.formatted(round, timestamp, round, round, timestamp);

  }

  private HttpResponse<String> post(
      final String path,
      final String body) throws Exception {

    final var request = HttpRequest
        .newBuilder(URI.create("http://localhost:"
            + port
            + path))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());

  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> read(
      final int round) throws Exception {

    final var answer = post("/official-api/v1/workflow", """
        { "mode": "All", "businessIds": [ "business-%d" ], "pageNumber": 0, "pageSize": 10 }
        """.formatted(round));
    assertThat(answer.statusCode()).isEqualTo(200);
    final var found = (List<Map<String, Object>>) jsonMapper
        .readValue(answer.body(), Map.class)
        .get("workflows");
    assertThat(found).hasSize(1);
    return found.get(0);

  }

  @Test
  void theEndOfAWorkflowStaysWhileSomebodyReadsIt() throws Exception {

    final List<Integer> lostEnds = new ArrayList<>();
    final ExecutorService readerThreads = Executors.newFixedThreadPool(READERS);
    try {
      for (int round = 0; round < ROUNDS; ++round) {
        assertThat(post("/bpms/api/v1_1/workflow/created", report(round, "2026-11-15T18:00:00Z")).statusCode())
            .isEqualTo(200);

        final var stop = new AtomicBoolean();
        final var currentRound = round;
        final var readers = new ArrayList<CompletableFuture<Void>>();
        for (int reader = 0; reader < READERS; ++reader) {
          readers.add(CompletableFuture.runAsync(() -> {
            try {
              while (!stop.get()) {
                read(currentRound);
              }
            } catch (Exception e) {
              throw new IllegalStateException(e);
            }
          }, readerThreads));
        }

        final var completed = post("/bpms/api/v1_1/workflow/workflow-"
            + round
            + "/completed", report(round, "2026-11-15T18:00:01Z"));
        stop.set(true);
        CompletableFuture.allOf(readers.toArray(CompletableFuture[]::new)).join();
        assertThat(completed.statusCode()).isEqualTo(200);

        if (read(round).get("endedAt") == null) {
          lostEnds.add(round);
        }
      }
    } finally {
      readerThreads.shutdownNow();
    }

    assertThat(lostEnds)
        .as("the rounds in which a read undid the reported end")
        .isEmpty();

  }

}
