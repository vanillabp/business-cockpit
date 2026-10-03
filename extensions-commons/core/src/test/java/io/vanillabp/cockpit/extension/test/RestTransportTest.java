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
import io.vanillabp.cockpit.extension.config.UiUriType;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.cockpit.extension.transport.RestTransport;
import io.vanillabp.integration.spi.PhaseTwoPermanentFailure;
import io.vanillabp.integration.spi.PhaseTwoRetryLater;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What the cockpit server receives over REST, asserted against a server of the test's own.
 * <p>
 * Reading the request instead of a mock of the client is what makes these assertions worth
 * having. The payload, the path and the method are what the two sides agreed on, and a
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

  private String responseBody;

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
              if (responseBody == null) {
                exchange.sendResponseHeaders(status, -1);
              } else {
                final var bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
              }
              exchange.close();
            });
    server.start();
    transport = new RestTransport(aCockpitServerAt(server.getAddress().getPort()));

  }

  /**
   * @param port Where the test's own server listens
   * @return The plainest configuration there is, an address and nothing else
   */
  static RestTransportConfiguration aCockpitServerAt(
      final int port) {

    return new RestTransportConfiguration(
        "http://localhost:%d".formatted(port), null, null, null, null, null, true, null, null, null);

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
    assertTrue(
        request.body().contains("\"admittedUsers\":[\"dora\"]"), request.body());
    assertTrue(request.body().contains("\"uiUriType\":\"WEBPACK_MF_REACT\""), request.body());
    assertTrue(request.body().contains("\"notificationDelivery\":\"FORCE\""), request.body());
    assertTrue(request.body().contains("\"amount\":250"), request.body());

  }

  @Test
  @DisplayName("The address of a task shown by another application travels as it was set")
  public void anExternalUserTaskCarriesItsOwnAddress() {

    final var event = EventFixture.userTask(UserTaskEventKind.CREATED);
    event.setUiUriType(UiUriType.EXTERNAL);
    event.setUiUriPath("https://tickets.example.com/ticket/4711");

    transport.publishUserTaskEvent(event);

    final var body = theRequest().body();
    assertTrue(body.contains("\"uiUriType\":\"EXTERNAL\""), body);
    assertTrue(
        body.contains("\"uiUriPath\":\"https://tickets.example.com/ticket/4711\""), body);

  }

  @Test
  @DisplayName("The address of a case shown by another application travels as it was set")
  public void anExternalWorkflowCarriesItsOwnAddress() {

    final var event = EventFixture.workflow(WorkflowEventKind.CREATED);
    event.setUiUriType(UiUriType.EXTERNAL);
    event.setUiUriPath("https://orders.example.com/order/4711");

    transport.publishWorkflowEvent(event);

    final var body = theRequest().body();
    assertTrue(body.contains("\"uiUriType\":\"EXTERNAL\""), body);
    assertTrue(
        body.contains("\"uiUriPath\":\"https://orders.example.com/order/4711\""), body);

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
  @DisplayName("A refusal without a reason names every cause a 400 can have, one step at a time")
  public void aRefusalWithoutAReasonNamesTheCauses() {

    status = 400;

    final var message = assertThrows(
        RuntimeException.class,
        () -> transport.publishWorkflowEvent(EventFixture.workflow(WorkflowEventKind.UPDATED)))
        .getMessage();

    assertTrue(message.contains("workflow 'workflow-1' as UPDATED"), message);
    assertTrue(message.contains("The server gave no reason."), message);
    assertTrue(message.contains("\n1. The workflow module and the cockpit server speak"), message);
    assertTrue(message.contains("\n2. The server could not read the report"), message);
    assertTrue(
        message.contains("\n3. The path and the body of the report name different"), message);
    assertTrue(message.contains("'Refusing a report about ...'"), message);
    assertTrue(message.contains("\n4. The server could not store the report"), message);
    assertFalse(
        message.contains("credentials"),
        "a 400 is no answer to credentials, so the message must not send anybody there");

  }

  @Test
  @DisplayName("A refusal with a reason carries the reason, cut to one short line")
  public void aRefusalWithAReasonCarriesIt() {

    status = 400;
    responseBody = "{\"detail\":\n  \"workflowId must not be null\"}"
        + "x".repeat(1_000);

    final var message = assertThrows(
        RuntimeException.class,
        () -> transport.publishWorkflowEvent(EventFixture.workflow(WorkflowEventKind.UPDATED)))
        .getMessage();

    assertTrue(
        message.contains("The server said: \"{\"detail\": \"workflowId must not be null\"}xxx"),
        message);
    assertTrue(message.contains("x...\"."), "a long reason is cut: "
        + message);
    assertFalse(message.contains("x".repeat(400)), "the whole body went into the message");
    assertTrue(message.contains("\n1. "), "the steps are still there: "
        + message);

  }

  @Test
  @DisplayName("A refusal of the credentials names the keys they are configured with")
  public void aRefusalOfTheCredentialsNamesTheirKeys() {

    status = 401;

    final var message = assertThrows(
        RuntimeException.class,
        () -> transport.registerWorkflowModule(EventFixture.workflowModule()))
        .getMessage();

    assertTrue(message.contains("'vanillabp.cockpit.rest.authentication.username'"), message);
    assertTrue(
        message.contains("'vanillabp.cockpit.rest.authentication.oauth.client-id'"), message);
    assertFalse(message.contains("could not read"), message);

  }

  @Test
  @DisplayName("A refusal of the caller names the role the server asks for")
  public void aForbiddenCallerIsToldTheRole() {

    status = 403;

    final var message = assertThrows(
        RuntimeException.class,
        () -> transport.registerWorkflowModule(EventFixture.workflowModule()))
        .getMessage();

    assertTrue(message.contains("the role 'BPMS-API'"), message);

  }

  @Test
  @DisplayName("Any other refusal points at the versions of the API")
  public void anotherRefusalPointsAtTheVersions() {

    status = 422;

    final var failure = assertThrows(
        RuntimeException.class,
        () -> transport.registerWorkflowModule(EventFixture.workflowModule()));

    assertTrue(PhaseTwoPermanentFailure.isPermanent(failure));
    assertTrue(failure.getMessage().contains("Check this:\n1. The workflow module"), failure.getMessage());

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
