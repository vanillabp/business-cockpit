package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTaskRepository;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowRepository;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A workflow module sends its reports through an outbox, and an outbox may send two reports about
 * the same workflow at the same time. A report which is retried can also arrive together with the
 * next one. The simulator has to keep what each of them says. If two of them read the stored row
 * at the same moment and each writes it back as a whole, the one which writes last undoes the
 * other. If the end arrives while the creation is still being stored, and the simulator refuses
 * it, the outbox tries again only half a minute later, and a test of the workflow module has
 * given up by then.
 *
 * <p>Each round sends two reports at the same moment. One round does not hit the gap every time,
 * so every test runs many of them. The reports go over HTTP, like a workflow module sends them.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:reports-which-arrive-together-are-all-kept",
    // the simulator does not start without a user to log in as
    "dev-shell-simulator.users[0].id=john"
})
class ReportsWhichArriveTogetherAreAllKeptTest {

  private static final int ROUNDS = 40;

  private final HttpClient client = HttpClient.newHttpClient();

  private ExecutorService senders;

  @Value("${local.server.port}")
  private int port;

  @Autowired
  private WorkflowRepository workflows;

  @Autowired
  private UserTaskRepository userTasks;

  @BeforeEach
  void startTheSenders() {

    senders = Executors.newFixedThreadPool(2);

  }

  @AfterEach
  void removeWhatWasReported() {

    senders.shutdownNow();
    workflows.deleteAll();
    userTasks.deleteAll();

  }

  private static String workflowReport(
      final int round,
      final String timestamp,
      final String status) {

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
          "details": { "status": "%s" },
          "accessibleToUsers": [ "john" ],
          "accessibleToGroups": [ "ADMIN" ]
        }
        """.formatted(round, timestamp, round, round, timestamp, status);

  }

  private static String userTaskReport(
      final int round,
      final String timestamp) {

    return """
        {
          "id": "event-%d-%s",
          "userTaskId": "task-%d",
          "workflowId": "workflow-%d",
          "timestamp": "%s",
          "workflowModuleId": "loan-approval",
          "bpmnProcessId": "loan_approval",
          "taskDefinition": "assessRisk",
          "title": { "en": "Assess the risk" },
          "uiUriPath": "/ui",
          "uiUriType": "WEBCOMPONENT",
          "candidateUsers": [ "john" ]
        }
        """.formatted(round, timestamp, round, round, timestamp);

  }

  private int post(
      final String path,
      final String body) throws Exception {

    final var request = HttpRequest
        .newBuilder(URI.create("http://localhost:"
            + port
            + path))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();

  }

  /**
   * Sends two reports at the same moment and waits for both answers.
   *
   * @return The two status codes
   */
  private List<Integer> sendTogether(
      final String firstPath,
      final String firstBody,
      final String secondPath,
      final String secondBody) {

    final var bothReady = new CyclicBarrier(2);
    final var first = CompletableFuture.supplyAsync(() -> postWhenBothAreReady(bothReady, firstPath, firstBody),
        senders);
    final var second = CompletableFuture.supplyAsync(() -> postWhenBothAreReady(bothReady, secondPath, secondBody),
        senders);
    return List.of(first.join(), second.join());

  }

  private int postWhenBothAreReady(
      final CyclicBarrier bothReady,
      final String path,
      final String body) {

    try {
      bothReady.await();
      return post(path, body);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

  }

  private boolean workflowHasEnded(
      final int round) {

    return workflows
        .findById("workflow-"
            + round)
        .map(workflow -> workflow.getEndedAt() != null)
        .orElse(false);

  }

  @Test
  void anEndStaysWhenAChangeOfTheWorkflowArrivesAtTheSameTime() throws Exception {

    final var refused = new ArrayList<Integer>();
    final var lostEnds = new ArrayList<Integer>();
    for (int round = 0; round < ROUNDS; ++round) {
      assertThat(post("/bpms/api/v1_1/workflow/created", workflowReport(round, "2026-11-15T18:00:00Z", "new")))
          .isEqualTo(200);

      final var answers = sendTogether(
          "/bpms/api/v1_1/workflow/workflow-"
              + round
              + "/updated",
          workflowReport(round, "2026-11-15T18:00:01Z", "changed"),
          "/bpms/api/v1_1/workflow/workflow-"
              + round
              + "/completed",
          workflowReport(round, "2026-11-15T18:00:02Z", "done"));

      if (answers.stream().anyMatch(status -> status != 200)) {
        refused.add(round);
      }
      if (!workflowHasEnded(round)) {
        lostEnds.add(round);
      }
    }

    assertThat(refused)
        .as("the rounds in which the simulator refused one of the two reports")
        .isEmpty();
    assertThat(lostEnds)
        .as("the rounds in which a change arriving at the same time undid the end")
        .isEmpty();

  }

  @Test
  void anEndWhichArrivesTogetherWithTheCreationIsKept() throws Exception {

    final var refused = new ArrayList<Integer>();
    final var lostEnds = new ArrayList<Integer>();
    final var lostCreations = new ArrayList<Integer>();
    for (int round = 0; round < ROUNDS; ++round) {
      final var answers = sendTogether(
          "/bpms/api/v1_1/workflow/created",
          workflowReport(round, "2026-11-15T18:00:00Z", "new"),
          "/bpms/api/v1_1/workflow/workflow-"
              + round
              + "/completed",
          workflowReport(round, "2026-11-15T18:00:02Z", "done"));

      if (answers.stream().anyMatch(status -> status != 200)) {
        refused.add(round);
      }
      if (!workflowHasEnded(round)) {
        lostEnds.add(round);
      }
      final var stored = workflows.findById("workflow-"
          + round);
      if (stored.isEmpty() || (stored.get().getUiUri() == null)) {
        lostCreations.add(round);
      }
    }

    assertThat(refused)
        .as("the rounds in which the simulator refused the creation or the end")
        .isEmpty();
    assertThat(lostEnds)
        .as("the rounds in which the creation undid the end, or the end was refused")
        .isEmpty();
    assertThat(lostCreations)
        .as("the rounds in which what only the creation says is missing")
        .isEmpty();

  }

  @Test
  void anEndOfAUserTaskStaysWhenAChangeArrivesAtTheSameTime() throws Exception {

    final var refused = new ArrayList<Integer>();
    final var lostEnds = new ArrayList<Integer>();
    for (int round = 0; round < ROUNDS; ++round) {
      assertThat(post("/bpms/api/v1_1/usertask/created", userTaskReport(round, "2026-11-15T18:00:00Z")))
          .isEqualTo(200);

      final var answers = sendTogether(
          "/bpms/api/v1_1/usertask/task-"
              + round
              + "/updated",
          userTaskReport(round, "2026-11-15T18:00:01Z"),
          "/bpms/api/v1_1/usertask/task-"
              + round
              + "/completed",
          userTaskReport(round, "2026-11-15T18:00:02Z"));

      if (answers.stream().anyMatch(status -> status != 200)) {
        refused.add(round);
      }
      final var ended = userTasks
          .findById("task-"
              + round)
          .map(userTask -> userTask.getEndedAt() != null)
          .orElse(false);
      if (!ended) {
        lostEnds.add(round);
      }
    }

    assertThat(refused)
        .as("the rounds in which the simulator refused one of the two reports")
        .isEmpty();
    assertThat(lostEnds)
        .as("the rounds in which a change arriving at the same time undid the end of the user task")
        .isEmpty();

  }

}
