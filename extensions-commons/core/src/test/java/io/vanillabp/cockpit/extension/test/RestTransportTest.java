package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.sun.net.httpserver.HttpServer;

import io.vanillabp.cockpit.extension.config.RestTransportConfiguration;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.cockpit.extension.transport.RestTransport;
import io.vanillabp.integration.spi.PhaseTwoPermanentFailure;
import io.vanillabp.integration.spi.PhaseTwoRetryLater;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What the cockpit server receives over REST, asserted against a server of the test's own.
 * <p>
 * Reading the request rather than a mock of the client is what makes these assertions worth
 * having: the payload, the path and the method are what the two sides agreed on, and a
 * generated client is exactly the place where an agreement is easy to break by accident.
 */
@ExtendWith(SuppressOutputExtension.class)
public class RestTransportTest {

  /** One request the test server received. */
  record Request(
                 String method,
                 String path,
                 String body) {
  }

  private HttpServer server;

  private final List<Request> received = new LinkedList<>();

  private RestTransport transport;

  private int status = 200;

  private String retryAfter;

  @BeforeEach
  public void startTheCockpitServer() throws IOException {

    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server
        .createContext(
            "/",
            exchange -> {
              final var body = new String(
                  exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
              received
                  .add(
                      new Request(
                          exchange.getRequestMethod(), exchange.getRequestURI().getPath(), body));
              if (retryAfter != null) {
                exchange.getResponseHeaders().add("Retry-After", retryAfter);
              }
              exchange.sendResponseHeaders(status, -1);
              exchange.close();
            });
    server.start();
    transport = new RestTransport(
        new RestTransportConfiguration(
            "http://localhost:%d".formatted(server.getAddress().getPort()), null, null));

  }

  @AfterEach
  public void stopTheCockpitServer() {

    server.stop(0);

  }

  private Request theRequest() {

    assertEquals(1, received.size(), "expected exactly one request");
    return received.getFirst();

  }

  @Test
  @DisplayName("A created user task is posted with everything the details provider filled")
  public void createdUserTaskCarriesEverything() {

    transport.publishUserTaskEvent(EventFixture.userTask(UserTaskEventKind.CREATED));

    final var request = theRequest();
    assertEquals("POST", request.method());
    assertEquals("/bpms/api/v1_1/usertask/created", request.path());
    assertTrue(request.body().contains("\"id\":\"event-1\""), request.body());
    assertTrue(request.body().contains("\"userTaskId\":\"task-1\""), request.body());
    assertTrue(request.body().contains("\"updated\":false"), request.body());
    assertTrue(request.body().contains("\"taskDefinition\":\"approve\""), request.body());
    assertTrue(request.body().contains("\"assignee\":\"anna\""), request.body());
    assertTrue(request.body().contains("\"candidateGroups\":[\"approvers\"]"), request.body());
    assertTrue(
        request.body().contains("\"excludedCandidateUsers\":[\"carl\"]"), request.body());
    assertTrue(request.body().contains("\"uiUriType\":\"WEBPACK_MF_REACT\""), request.body());
    assertTrue(request.body().contains("\"notificationDelivery\":\"FORCE\""), request.body());
    assertTrue(request.body().contains("\"amount\":250"), request.body());

  }

  @Test
  @DisplayName("The other three kinds of user-task event go to their own endpoints")
  public void userTaskLifecycleGoesToItsOwnEndpoints() {

    transport.publishUserTaskEvent(EventFixture.userTask(UserTaskEventKind.UPDATED));
    transport.publishUserTaskEvent(EventFixture.userTask(UserTaskEventKind.COMPLETED));
    transport.publishUserTaskEvent(EventFixture.userTask(UserTaskEventKind.CANCELED));

    assertEquals(
        List
            .of(
                "/bpms/api/v1_1/usertask/task-1/updated",
                "/bpms/api/v1_1/usertask/task-1/completed",
                "/bpms/api/v1_1/usertask/task-1/cancelled"),
        received.stream().map(Request::path).toList());
    assertTrue(received.getFirst().body().contains("\"updated\":true"));

  }

  @Test
  @DisplayName("Workflow events go to their four endpoints")
  public void workflowEventsGoToTheirEndpoints() {

    transport.publishWorkflowEvent(EventFixture.workflow(WorkflowEventKind.CREATED));
    transport.publishWorkflowEvent(EventFixture.workflow(WorkflowEventKind.UPDATED));
    transport.publishWorkflowEvent(EventFixture.workflow(WorkflowEventKind.COMPLETED));
    transport.publishWorkflowEvent(EventFixture.workflow(WorkflowEventKind.CANCELLED));

    assertEquals(
        List
            .of(
                "/bpms/api/v1_1/workflow/created",
                "/bpms/api/v1_1/workflow/workflow-1/updated",
                "/bpms/api/v1_1/workflow/workflow-1/completed",
                "/bpms/api/v1_1/workflow/workflow-1/cancelled"),
        received.stream().map(Request::path).toList());
    assertTrue(received.getFirst().body().contains("\"businessId\":\"4711\""));
    assertTrue(received.getFirst().body().contains("\"accessibleToGroups\":[\"approvers\"]"));

  }

  @Test
  @DisplayName("A workflow module is registered under its own id")
  public void workflowModuleIsRegistered() {

    transport.registerWorkflowModule(EventFixture.workflowModule());

    final var request = theRequest();
    assertEquals("/bpms/api/v1_1/workflow-module/test-module", request.path());
    assertTrue(request.body().contains("\"uri\":\"http://localhost:8081\""), request.body());
    assertTrue(request.body().contains("\"group\":\"TEAM_LEAD\""), request.body());
    assertTrue(request.body().contains("\"targets\":[\"TEAM_MEMBER\"]"), request.body());
    assertTrue(request.body().contains("/task-provider"), request.body());

  }

  @Test
  @DisplayName("A cockpit server which is not taking reports has the entry given back to it")
  public void anUnavailableServerHasTheReportRepeated() {

    status = 503;
    retryAfter = "7";

    final var failure = assertThrows(
        RuntimeException.class,
        () -> transport.publishUserTaskEvent(EventFixture.userTask(UserTaskEventKind.CREATED)));

    assertFalse(
        PhaseTwoPermanentFailure.isPermanent(failure),
        "an unavailable server ended the report instead of having it repeated");
    final var waiting = PhaseTwoRetryLater.retryAfter(failure);
    assertNotNull(waiting, "the server named a moment to come back and nobody read it");
    assertTrue(
        (waiting.getSeconds() > 0) && (waiting.getSeconds() <= 7),
        "the report waits %s, which is not the seven seconds the server asked for"
            .formatted(waiting));

  }

  @Test
  @DisplayName("A cockpit server which refuses the report itself ends the entry")
  public void aRefusedReportIsGivenUp() {

    status = 400;

    final var failure = assertThrows(
        RuntimeException.class,
        () -> transport.publishUserTaskEvent(EventFixture.userTask(UserTaskEventKind.CREATED)));

    assertTrue(
        PhaseTwoPermanentFailure.isPermanent(failure),
        "a report the server refuses would have been repeated forever");
    assertTrue(failure.getMessage().contains("REST API at"), failure.getMessage());

  }

  @Test
  @DisplayName("A cockpit server which failed on its own side has the entry repeated")
  public void aServerFailureHasTheReportRepeated() {

    status = 500;

    final var failure = assertThrows(
        RuntimeException.class,
        () -> transport.publishUserTaskEvent(EventFixture.userTask(UserTaskEventKind.CREATED)));

    assertFalse(
        PhaseTwoPermanentFailure.isPermanent(failure),
        "a server which failed on its own side ended the report");

  }

  @Test
  @DisplayName("The transport says where it sends, for a message about a failure")
  public void theTransportSaysWhereItSends() {

    assertTrue(transport.describe().contains("REST"), transport.describe());

  }

}
