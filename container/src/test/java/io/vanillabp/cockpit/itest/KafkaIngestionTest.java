package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.google.protobuf.Timestamp;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.BcEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCreatedOrUpdatedEvent;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Feeds protobuf events into the Kafka topics the container consumes - serialized the same way the
 * adapters' *KafkaPublishing classes do it (record key = entity id, record value = a BcEvent
 * wrapper) - and verifies the read side through the GUI API. Consumption is asynchronous, so all
 * verifications poll with a timeout.
 */
class KafkaIngestionTest extends ItestBase {

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
        moduleId = unique("kafka-module");
        token = unique("token");
        registerWorkflowModule(moduleId, "http://localhost:65000");
        cookie = loginToGui(USER_MARTIN);
    }

    private static void produce(
            final String topic,
            final String key,
            final BcEvent event) {

        try {
            producer.send(new ProducerRecord<>(topic, key, event.toByteArray())).get();
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

    private UserTaskCreatedOrUpdatedEvent.Builder userTaskEvent(
            final String userTaskId) {

        return UserTaskCreatedOrUpdatedEvent
                .newBuilder()
                .setId(UUID.randomUUID().toString())
                .setApiVersion("1.1")
                .setUserTaskId(userTaskId)
                .setTimestamp(now())
                .setBpmnProcessId("taxi-ride")
                .setTaskDefinition("do-ride")
                .putTitle("en", "Do ride (via Kafka)")
                .setWorkflowModuleId(moduleId)
                .setUiUriPath("/remoteEntry.js")
                .setUiUriType("WEBPACK_MF_REACT")
                .setAssignee(USER_MARTIN)
                .setDetailsFulltextSearch(token);

    }

    @Test
    void userTaskCreatedEventShowsUpInTasklist() {

        final var userTaskId = unique("task");
        produce(KAFKA_USER_TASK_TOPIC, userTaskId, BcEvent
                .newBuilder()
                .setUserTaskCreatedV11(userTaskEvent(userTaskId)
                        .addCandidateGroups(GROUP_OF_MARTIN))
                .build());

        await().untilAsserted(() -> {
            final var list = userTaskList(cookie, token, "OpenTasks");
            assertThat(list.read("$.userTasks[*].id", List.class)).containsExactly(userTaskId);
        });

        final var task = json(guiGet(cookie, "/usertask/" + userTaskId));
        assertThat(task.read("$.title.en", String.class)).isEqualTo("Do ride (via Kafka)");
        assertThat(task.read("$.assignee.id", String.class)).isEqualTo(USER_MARTIN);
        assertThat(task.read("$.candidateGroups[*].id", List.class))
                .containsExactly(GROUP_OF_MARTIN);
        assertThat(task.read("$.uiUri", String.class))
                .isEqualTo("/wm/" + moduleId + "/remoteEntry.js");

    }

    @Test
    void userTaskUpdatedEventChangesTheTask() {

        final var userTaskId = unique("task");
        produce(KAFKA_USER_TASK_TOPIC, userTaskId, BcEvent
                .newBuilder()
                .setUserTaskCreatedV11(userTaskEvent(userTaskId))
                .build());
        await().untilAsserted(() ->
                assertThat(guiGet(cookie, "/usertask/" + userTaskId).statusCode()).isEqualTo(200));

        produce(KAFKA_USER_TASK_TOPIC, userTaskId, BcEvent
                .newBuilder()
                .setUserTaskUpdatedV11(userTaskEvent(userTaskId)
                        .setUpdated(true)
                        .putTitle("en", "Do ride (updated via Kafka)"))
                .build());

        await().untilAsserted(() ->
                assertThat(json(guiGet(cookie, "/usertask/" + userTaskId))
                        .read("$.title.en", String.class))
                        .isEqualTo("Do ride (updated via Kafka)"));

    }

    @Test
    void userTaskCompletedEventEndsTheTask() {

        final var userTaskId = unique("task");
        produce(KAFKA_USER_TASK_TOPIC, userTaskId, BcEvent
                .newBuilder()
                .setUserTaskCreatedV11(userTaskEvent(userTaskId))
                .build());
        await().untilAsserted(() ->
                assertThat(guiGet(cookie, "/usertask/" + userTaskId).statusCode()).isEqualTo(200));

        produce(KAFKA_USER_TASK_TOPIC, userTaskId, BcEvent
                .newBuilder()
                .setUserTaskCompletedV11(userTaskEvent(userTaskId).setUpdated(true))
                .build());

        await().untilAsserted(() ->
                assertThat(json(guiGet(cookie, "/usertask/" + userTaskId))
                        .read("$.endedAt", String.class))
                        .isNotNull());

    }

    @Test
    void userTaskCancelledEventEndsTheTask() {

        final var userTaskId = unique("task");
        produce(KAFKA_USER_TASK_TOPIC, userTaskId, BcEvent
                .newBuilder()
                .setUserTaskCreatedV11(userTaskEvent(userTaskId))
                .build());
        await().untilAsserted(() ->
                assertThat(guiGet(cookie, "/usertask/" + userTaskId).statusCode()).isEqualTo(200));

        produce(KAFKA_USER_TASK_TOPIC, userTaskId, BcEvent
                .newBuilder()
                .setUserTaskCancelledV11(userTaskEvent(userTaskId)
                        .setUpdated(true)
                        .setComment("cancelled via Kafka"))
                .build());

        await().untilAsserted(() ->
                assertThat(json(guiGet(cookie, "/usertask/" + userTaskId))
                        .read("$.endedAt", String.class))
                        .isNotNull());

    }

    @Test
    void workflowCreatedEventShowsUpInWorkflowlist() {

        final var workflowId = unique("workflow");
        produce(KAFKA_WORKFLOW_TOPIC, workflowId, BcEvent
                .newBuilder()
                .setWorkflowCreatedV11(WorkflowCreatedOrUpdatedEvent
                        .newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setApiVersion("1.1")
                        .setWorkflowId(workflowId)
                        .setTimestamp(now())
                        .setBpmnProcessId("taxi-ride")
                        .putTitle("en", "Ride request (via Kafka)")
                        .setWorkflowModuleId(moduleId)
                        .setUiUriPath("/remoteEntry.js")
                        .setUiUriType("WEBPACK_MF_REACT")
                        .setDetailsFulltextSearch(token))
                .build());

        await().untilAsserted(() -> {
            final var list = workflowList(cookie, token, "Active");
            assertThat(list.read("$.workflows[*].id", List.class)).containsExactly(workflowId);
        });
        assertThat(json(guiGet(cookie, "/workflow/" + workflowId))
                .read("$.title.en", String.class))
                .isEqualTo("Ride request (via Kafka)");

    }

    @Test
    void workflowModuleRegistrationEventRegistersTheModule() {

        final var kafkaModuleId = unique("kafka-registered-module");
        produce(KAFKA_WORKFLOW_MODULE_TOPIC, kafkaModuleId, BcEvent
                .newBuilder()
                .setRegisterWorkflowModule(RegisterWorkflowModuleEvent
                        .newBuilder()
                        .setWorkflowModuleId(kafkaModuleId)
                        .setUri("http://localhost:65001")
                        .setTaskProviderApiUriPath("/task-provider/v1"))
                .build());

        await().untilAsserted(() -> {
            final var response = guiGet(cookie, "/workflow-module/" + kafkaModuleId);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(json(response).read("$.uri", String.class))
                    .isEqualTo("/wm/" + kafkaModuleId);
        });

    }

}
