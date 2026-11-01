package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.protobuf.Timestamp;

import io.vanillabp.cockpit.bpms.api.protobuf.v1.BcEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCancelledEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCompletedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCreatedOrUpdatedEvent;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A workflow module reports a completion and a cancellation as two different reports. The
 * cockpit keeps the difference as {@code endReason}, and the GUI API shows it. Each way a report
 * comes in is checked with each end: version 1 and version 1.1 of the REST API, and both versions
 * over Kafka.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class ACaseSaysWhyItEndedTest extends ItestBase {

  private static KafkaProducer<String, byte[]> producer;

  private String moduleId;
  private String token;
  private String cookie;

  @BeforeAll
  static void startProducer() {
    producer = new KafkaProducer<>(Map.of(
        "bootstrap.servers", kafkaBootstrapServers(),
        "key.serializer", StringSerializer.class.getName(),
        "value.serializer", ByteArraySerializer.class.getName()));
  }

  @AfterAll
  static void stopProducer() {
    producer.close();
  }

  @BeforeEach
  void registerModuleAndLogin() {
    moduleId = unique("end-reason-module");
    token = unique("token");
    registerWorkflowModule(moduleId, "http://localhost:65000");
    cookie = loginToGui(USER_MARTIN);
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "completed", "cancelled"
  })
  void aReportOfVersion1SaysHowTheCaseEnded(
      final String end) {

    final var workflowId = aRunningCase();

    final var response = send(HttpRequest
        .newBuilder(URI.create(url("/bpms/api/v1/workflow/"
            + workflowId
            + "/"
            + end)))
        .header("Authorization", basicAuth(BPMS_API_USER, BPMS_API_PASSWORD))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString("""
            { "id": "%s", "workflowId": "%s", "timestamp": "%s" }
            """.formatted(unique("event"), workflowId, isoNow())))
        .build());
    assertThat(response.statusCode()).isEqualTo(200);

    assertThat(endReasonOf(workflowId)).isEqualTo(reasonFor(end));

  }

  @ParameterizedTest
  @ValueSource(strings = {
      "completed", "cancelled"
  })
  void aReportOfVersion1_1SaysHowTheCaseEnded(
      final String end) {

    final var workflowId = aRunningCase();

    final var response = bpmsV1_1("/workflow/"
        + workflowId
        + "/"
        + end, workflowPayload(workflowId));
    assertThat(response.statusCode()).isEqualTo(200);

    assertThat(endReasonOf(workflowId)).isEqualTo(reasonFor(end));

  }

  @ParameterizedTest
  @ValueSource(strings = {
      "completed", "cancelled"
  })
  void aKafkaReportOfVersion1SaysHowTheCaseEnded(
      final String end) {

    final var workflowId = aRunningCase();

    final var event = BcEvent.newBuilder();
    if ("completed".equals(end)) {
      event.setWorkflowCompleted(WorkflowCompletedEvent
          .newBuilder()
          .setId(UUID.randomUUID().toString())
          .setApiVersion("1")
          .setWorkflowId(workflowId)
          .setTimestamp(now()));
    } else {
      event.setWorkflowCancelled(WorkflowCancelledEvent
          .newBuilder()
          .setId(UUID.randomUUID().toString())
          .setApiVersion("1")
          .setWorkflowId(workflowId)
          .setTimestamp(now()));
    }
    produce(workflowId, event.build());

    await().untilAsserted(() -> assertThat(endReasonOf(workflowId)).isEqualTo(reasonFor(end)));

  }

  @ParameterizedTest
  @ValueSource(strings = {
      "completed", "cancelled"
  })
  void aKafkaReportOfVersion1_1SaysHowTheCaseEnded(
      final String end) {

    final var workflowId = aRunningCase();

    final var ended = WorkflowCreatedOrUpdatedEvent
        .newBuilder()
        .setId(UUID.randomUUID().toString())
        .setApiVersion("1.1")
        .setWorkflowId(workflowId)
        .setTimestamp(now())
        .setBpmnProcessId("taxi-ride")
        .putTitle("en", "Ride request")
        .setWorkflowModuleId(moduleId)
        .setUiUriPath("/remoteEntry.js")
        .setUiUriType("WEBPACK_MF_REACT")
        .setDetailsFulltextSearch(token);
    final Consumer<BcEvent.Builder> setEnd = "completed".equals(end)
        ? event -> event.setWorkflowCompletedV11(ended)
        : event -> event.setWorkflowCancelledV11(ended);
    final var event = BcEvent.newBuilder();
    setEnd.accept(event);
    produce(workflowId, event.build());

    await().untilAsserted(() -> assertThat(endReasonOf(workflowId)).isEqualTo(reasonFor(end)));

  }

  @Test
  void aRunningCaseHasNoEndReason() {

    final var workflowId = aRunningCase();

    assertThat(endReasonOf(workflowId)).isNull();

  }

  @Test
  void aUserTaskShowsHowItEnded() {

    final var completed = reportUserTask(moduleId, token, unique("workflow"), """
        "assignee": "%s"
        """.formatted(USER_MARTIN));
    reportUserTaskCompleted(moduleId, token, completed);
    final var cancelled = reportUserTask(moduleId, token, unique("workflow"), """
        "assignee": "%s"
        """.formatted(USER_MARTIN));
    final var response = bpmsV1_1("/usertask/"
        + cancelled
        + "/cancelled", """
            {
              "id": "%s",
              "userTaskId": "%s",
              "timestamp": "%s",
              "workflowModuleId": "%s",
              "bpmnProcessId": "taxi-ride",
              "title": { "en": "Do ride" },
              "taskDefinition": "do-ride",
              "uiUriPath": "/remoteEntry.js",
              "uiUriType": "WEBPACK_MF_REACT"
            }
            """.formatted(unique("event"), cancelled, isoNow(), moduleId));
    assertThat(response.statusCode()).isEqualTo(200);

    assertThat(json(guiGet(cookie, "/usertask/"
        + completed)).read("$.endReason", String.class)).isEqualTo("COMPLETED");
    assertThat(json(guiGet(cookie, "/usertask/"
        + cancelled)).read("$.endReason", String.class)).isEqualTo("CANCELLED");

  }

  private String aRunningCase() {

    final var workflowId = unique("workflow");
    assertThat(bpmsV1_1("/workflow/created", workflowPayload(workflowId)).statusCode())
        .isEqualTo(200);
    return workflowId;

  }

  private String workflowPayload(
      final String workflowId) {

    return """
        {
          "id": "%s",
          "workflowId": "%s",
          "timestamp": "%s",
          "workflowModuleId": "%s",
          "bpmnProcessId": "taxi-ride",
          "bpmnProcessVersion": "1",
          "initiator": "martin",
          "title": { "en": "Ride request" },
          "uiUriPath": "/remoteEntry.js",
          "uiUriType": "WEBPACK_MF_REACT",
          "detailsFulltextSearch": "%s"
        }
        """.formatted(unique("event"), workflowId, isoNow(), moduleId, token);

  }

  private String endReasonOf(
      final String workflowId) {

    return json(guiGet(cookie, "/workflow/"
        + workflowId)).read("$.endReason", String.class);

  }

  private static String reasonFor(
      final String end) {
    return "completed".equals(end) ? "COMPLETED" : "CANCELLED";
  }

  private static void produce(
      final String key,
      final BcEvent event) {

    try {
      producer.send(new ProducerRecord<>(KAFKA_WORKFLOW_TOPIC, key, event.toByteArray())).get();
    } catch (Exception e) {
      throw new IllegalStateException("Could not produce Kafka event", e);
    }

  }

  private static Timestamp now() {
    final var instant = Instant.now();
    return Timestamp
        .newBuilder()
        .setSeconds(instant.getEpochSecond())
        .setNanos(instant.getNano())
        .build();
  }

}
