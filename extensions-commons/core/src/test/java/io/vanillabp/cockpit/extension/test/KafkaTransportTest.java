package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.protobuf.InvalidProtocolBufferException;

import io.vanillabp.cockpit.bpms.api.protobuf.v1.BcEvent;
import io.vanillabp.cockpit.extension.BusinessCockpitAssembly;
import io.vanillabp.cockpit.extension.config.KafkaTransportConfiguration;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.cockpit.extension.transport.KafkaTransport;
import io.vanillabp.integration.spi.PhaseTwoPermanentFailure;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What lands on the Kafka topics: which topic, which key, and which slot of the envelope the
 * cockpit server reads the message out of.
 * <p>
 * The producer is Kafka's own test double rather than a broker in a container. What is worth
 * asserting here is the message, and a broker would only carry the same bytes back at the price
 * of a container per run.
 */
@ExtendWith(SuppressOutputExtension.class)
public class KafkaTransportTest {

  private static final KafkaTransportConfiguration CONFIGURATION = new KafkaTransportConfiguration(
      "broker:9092", "user-task", "workflow", "workflow-module", Map.of());

  private final MockProducer<String, byte[]> producer = new MockProducer<>(
      true, null, new StringSerializer(), new ByteArraySerializer());

  private final KafkaTransport transport = new KafkaTransport(
      CONFIGURATION, BusinessCockpitAssembly.objectMapper(), producer);

  private BcEvent theEnvelope(
      final String expectedTopic,
      final String expectedKey) throws InvalidProtocolBufferException {

    assertEquals(1, producer.history().size(), "expected exactly one record");
    final var record = producer.history().getFirst();
    assertEquals(expectedTopic, record.topic());
    assertEquals(expectedKey, record.key());
    return BcEvent.parseFrom(record.value());

  }

  @Test
  @DisplayName("A created user task lands on the user-task topic, keyed by the task")
  public void createdUserTaskIsSent() throws Exception {

    transport.publishUserTaskEvent(EventFixture.userTask(UserTaskEventKind.CREATED));

    final var envelope = theEnvelope("user-task", "task-1");
    assertTrue(envelope.hasUserTaskCreatedV11());
    final var message = envelope.getUserTaskCreatedV11();
    assertEquals("event-1", message.getId());
    assertEquals("task-1", message.getUserTaskId());
    assertEquals("approve", message.getTaskDefinition());
    assertEquals("WEBPACK_MF_REACT", message.getUiUriType());
    assertEquals("anna", message.getAssignee());
    assertEquals(List.of("approvers"), message.getCandidateGroupsList());
    assertEquals(List.of("carl"), message.getExcludedCandidateUsersList());
    assertEquals("Approve order 4711", message.getTitleMap().get("en"));
    assertFalse(message.getUpdated());
    assertEquals(
        EventFixture.TIMESTAMP.toEpochSecond(), message.getTimestamp().getSeconds());

  }

  @Test
  @DisplayName("Each kind of user-task event uses its own slot of the envelope")
  public void everyKindHasItsSlot() throws Exception {

    transport.publishUserTaskEvent(EventFixture.userTask(UserTaskEventKind.UPDATED));
    assertTrue(BcEvent.parseFrom(producer.history().getFirst().value()).hasUserTaskUpdatedV11());
    producer.clear();

    transport.publishUserTaskEvent(EventFixture.userTask(UserTaskEventKind.COMPLETED));
    assertTrue(
        BcEvent.parseFrom(producer.history().getFirst().value()).hasUserTaskCompletedV11());
    producer.clear();

    transport.publishUserTaskEvent(EventFixture.userTask(UserTaskEventKind.CANCELED));
    assertTrue(
        BcEvent.parseFrom(producer.history().getFirst().value()).hasUserTaskCancelledV11());

  }

  @Test
  @DisplayName("Business data becomes the nested shape the cockpit reads")
  public void detailsBecomeTheNestedShape() throws Exception {

    transport.publishUserTaskEvent(EventFixture.userTask(UserTaskEventKind.CREATED));

    final var details = theEnvelope("user-task", "task-1")
        .getUserTaskCreatedV11()
        .getDetails()
        .getDetailsMap();
    assertFalse(details.get("amount").getIsArray());
    assertEquals("250", details.get("amount").getArrayValues(0).getNumericValue());
    assertEquals("EUR", details.get("currency").getArrayValues(0).getStringValue());
    assertTrue(details.get("urgent").getArrayValues(0).getBoolValue());
    assertTrue(details.get("tags").getIsArray());
    assertEquals(2, details.get("tags").getArrayValuesCount());
    assertEquals(
        "Anna",
        details
            .get("customer")
            .getArrayValues(0)
            .getMapValue()
            .getDetailsMap()
            .get("name")
            .getArrayValues(0)
            .getStringValue());
    // a detail whose value is null is not transported at all - the cockpit reads an absent
    // value and a null value the same way, and leaving it out keeps the message smaller
    assertFalse(details.containsKey("cancelledAt"));

  }

  @Test
  @DisplayName("A workflow event lands on the workflow topic, keyed by the workflow")
  public void workflowEventIsSent() throws Exception {

    transport.publishWorkflowEvent(EventFixture.workflow(WorkflowEventKind.COMPLETED));

    final var envelope = theEnvelope("workflow", "workflow-1");
    assertTrue(envelope.hasWorkflowCompletedV11());
    assertEquals("4711", envelope.getWorkflowCompletedV11().getBusinessId());
    assertEquals(
        List.of("approvers"), envelope.getWorkflowCompletedV11().getAccessibleToGroupsList());

  }

  @Test
  @DisplayName("A workflow module is registered on its topic, keyed by the module")
  public void workflowModuleIsRegistered() throws Exception {

    transport.registerWorkflowModule(EventFixture.workflowModule());

    final var envelope = theEnvelope("workflow-module", "test-module");
    assertTrue(envelope.hasRegisterWorkflowModule());
    final var message = envelope.getRegisterWorkflowModule();
    assertEquals("http://localhost:8081", message.getUri());
    assertEquals("TEAM_LEAD", message.getGroupHierarchy(0).getGroup());
    assertEquals(List.of("TEAM_MEMBER"), message.getGroupHierarchy(0).getTargetList());

  }

  @Test
  @DisplayName("A broker which is busy makes the transport throw, so the entry is retried")
  public void aRefusedRecordIsRetried() {

    producer.sendException = new TimeoutException("the broker did not answer in time");

    final var failure = assertThrows(
        RuntimeException.class,
        () -> transport.publishUserTaskEvent(EventFixture.userTask(UserTaskEventKind.CREATED)));

    assertFalse(
        PhaseTwoPermanentFailure.isPermanent(failure),
        "a busy broker ended the report instead of having it repeated");
    assertTrue(failure.getMessage().contains("broker:9092"), failure.getMessage());

  }

  @Test
  @DisplayName("A record the broker will never accept ends the report instead of being repeated")
  public void aRecordNobodyWillAcceptIsGivenUp() {

    producer.sendException = new RecordTooLargeException("the record is larger than allowed");

    final var failure = assertThrows(
        RuntimeException.class,
        () -> transport.publishUserTaskEvent(EventFixture.userTask(UserTaskEventKind.CREATED)));

    assertTrue(
        PhaseTwoPermanentFailure.isPermanent(failure),
        "a record which cannot be accepted would have been repeated forever");

  }

  @Test
  @DisplayName("The transport says where it sends, for a message about a failure")
  public void theTransportSaysWhereItSends() {

    assertTrue(transport.describe().contains("broker:9092"), transport.describe());

  }

}
