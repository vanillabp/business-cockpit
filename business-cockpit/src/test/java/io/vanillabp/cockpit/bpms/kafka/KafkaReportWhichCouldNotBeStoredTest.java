package io.vanillabp.cockpit.bpms.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.protobuf.Timestamp;
import io.vanillabp.cockpit.bpms.BpmsApiProperties;
import io.vanillabp.cockpit.bpms.LoggedErrors;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.BcEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowlist.model.WorkflowRepository;
import io.vanillabp.cockpit.workflowmodules.WorkflowModuleService;
import io.vanillabp.integration.test.utils.CapturedOutput;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.bson.BsonMaximumSizeExceededException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.testcontainers.kafka.KafkaContainer;

/**
 * What the cockpit does with a report from Kafka which it could not store, through a real broker
 * and the listener containers Spring builds.
 * <p>
 * A report which is not stored has to come again, like a report over REST answered with 503. A
 * record which can never be read, or whose report MongoDB refuses every time, must not hold up the
 * records behind it. Each attempt which failed is logged as one error. Both are about what the
 * listener container does once the listener threw, and that is what a broker and a real container
 * show and a call of the listener method does not.
 * <p>
 * MongoDB is a double here, because what is tested is a save which fails on purpose. The repository
 * keeps what it stores in a map and fails the first saves or reads, as a MongoDB which is gone for
 * a moment would. The two services which store a report are the real ones, so a failed save is
 * answered the way the cockpit answers it.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class KafkaReportWhichCouldNotBeStoredTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-10-03T08:00:00Z");
    private static final OffsetDateTime CHANGED_AT = OffsetDateTime.parse("2026-10-03T09:00:00Z");

    /** Long enough for two failures and the backoff after them, which is one and two seconds. */
    private static final Duration ARRIVAL = Duration.ofSeconds(60);

    private static final KafkaContainer BROKER = new KafkaContainer("apache/kafka:3.8.0");

    private static KafkaProducer<String, byte[]> producer;

    private final Map<String, UserTask> storedTasks = new ConcurrentHashMap<>();

    /** The ids of the tasks in the order their saves went through. */
    private final List<String> savedInOrder = new CopyOnWriteArrayList<>();

    private final AtomicInteger savesToFail = new AtomicInteger();

    private final AtomicInteger readsToFail = new AtomicInteger();

    /** The ids of the tasks MongoDB refuses every time, as it refuses a document over 16 MB. */
    private final Set<String> tooLarge = ConcurrentHashMap.newKeySet();

    private String userTaskTopic;

    private ApplicationContextRunner cockpit;

    @BeforeAll
    static void startBroker() {

        BROKER.start();
        producer = new KafkaProducer<>(Map.of(
                "bootstrap.servers", BROKER.getBootstrapServers(),
                "key.serializer", StringSerializer.class.getName(),
                "value.serializer", ByteArraySerializer.class.getName()));

    }

    @AfterAll
    static void stopBroker() {

        if (producer != null) {
            producer.close();
        }
        BROKER.stop();

    }

    @BeforeEach
    void buildTheCockpit() {

        // each test gets topics and a consumer group of its own, so no record of one test is
        // left for the next
        final var unique = UUID.randomUUID().toString();
        userTaskTopic = "user-task-" + unique;

        final var userTaskRepository = mock(UserTaskRepository.class);
        when(userTaskRepository.findById(anyString())).thenAnswer(invocation -> {
            if (readsToFail.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
                throw new DataAccessResourceFailureException("MongoDB is gone for a moment");
            }
            return Optional.ofNullable(storedTasks.get(invocation.<String>getArgument(0)));
        });
        when(userTaskRepository.save(any(UserTask.class))).thenAnswer(invocation -> {
            if (savesToFail.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
                throw new DataAccessResourceFailureException("MongoDB is gone for a moment");
            }
            final UserTask task = invocation.getArgument(0);
            if (tooLarge.contains(task.getId())) {
                // what the driver throws, unchanged by Spring
                throw new BsonMaximumSizeExceededException("Payload document size is larger than maximum of 16793600.");
            }
            storedTasks.put(task.getId(), task);
            savedInOrder.add(task.getId());
            return task;
        });

        // the services are built by hand, with only what storing a report needs, and handed to the
        // context as they are. Spring injects nothing into a registered singleton and calls none of
        // its lifecycle methods, which would want MongoDB
        final var userTaskService = new UserTaskService();
        set(UserTaskService.class, userTaskService, "userTasks", userTaskRepository);
        set(UserTaskService.class, userTaskService, "logger", LoggerFactory.getLogger(UserTaskService.class));
        final var workflowlistService = new WorkflowlistService();
        set(WorkflowlistService.class, workflowlistService, "workflowRepository", mock(WorkflowRepository.class));
        set(WorkflowlistService.class, workflowlistService, "logger", LoggerFactory.getLogger(WorkflowlistService.class));

        cockpit = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        KafkaAutoConfiguration.class,
                        KafkaConfiguration.class))
                .withUserConfiguration(WhatTheIngestionNeeds.class)
                .withInitializer(context -> {
                    final var beanFactory = context.getBeanFactory();
                    beanFactory.registerSingleton("userTaskService", userTaskService);
                    beanFactory.registerSingleton("workflowlistService", workflowlistService);
                    beanFactory.registerSingleton("workflowModuleService", mock(WorkflowModuleService.class));
                })
                .withPropertyValues(
                        "workerId=test",
                        "spring.kafka.bootstrap-servers=" + BROKER.getBootstrapServers(),
                        "spring.kafka.consumer.auto-offset-reset=earliest",
                        "bpms-api.kafka.group-id-suffix=" + unique,
                        "bpms-api.kafka.topics.user-task=" + userTaskTopic,
                        "bpms-api.kafka.topics.workflow=workflow-" + unique,
                        "bpms-api.kafka.topics.workflow-module=workflow-module-" + unique);

    }

    private static void set(
            final Class<?> declaringClass,
            final Object target,
            final String field,
            final Object value) {

        try {
            final var declaredField = declaringClass.getDeclaredField(field);
            declaredField.setAccessible(true);
            declaredField.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }

    }

    /**
     * The Kafka settings and the mapping of persons and groups, which the mappers of the
     * ingestion are wired with.
     */
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(BpmsApiProperties.class)
    static class WhatTheIngestionNeeds {

        @Bean
        PersonAndGroupMapper personAndGroupMapper() {

            final var personAndGroupMapper = mock(PersonAndGroupMapper.class);
            when(personAndGroupMapper.toModelPerson(anyString())).thenAnswer(invocation -> {
                final var person = new Person();
                person.setId(invocation.getArgument(0));
                return person;
            });
            when(personAndGroupMapper.toModelGroup(anyString())).thenAnswer(invocation -> {
                final var group = new Group();
                group.setId(invocation.getArgument(0));
                return group;
            });
            return personAndGroupMapper;

        }

    }

    private void produce(
            final String key,
            final byte[] value) {

        try {
            producer.send(new ProducerRecord<>(userTaskTopic, key, value)).get();
        } catch (Exception e) {
            throw new IllegalStateException("Could not produce a Kafka record", e);
        }

    }

    private static Timestamp protobuf(
            final OffsetDateTime timestamp) {

        return Timestamp
                .newBuilder()
                .setSeconds(timestamp.toEpochSecond())
                .setNanos(timestamp.getNano())
                .build();

    }

    private static UserTaskCreatedOrUpdatedEvent.Builder reportedTask(
            final String userTaskId,
            final OffsetDateTime timestamp,
            final String title) {

        return UserTaskCreatedOrUpdatedEvent
                .newBuilder()
                .setId(UUID.randomUUID().toString())
                .setApiVersion("1.1")
                .setUserTaskId(userTaskId)
                .setTimestamp(protobuf(timestamp))
                .setWorkflowModuleId("taxi-ride")
                .setBpmnProcessId("ride")
                .setWorkflowId("workflow-1")
                .setTaskDefinition("assign-driver")
                .putTitle("en", title)
                .setUiUriPath("/ui")
                .setUiUriType("WEBPACK_MF_REACT");

    }

    private void produceCreation(
            final String userTaskId,
            final String title) {

        produce(userTaskId, BcEvent
                .newBuilder()
                .setUserTaskCreatedV11(reportedTask(userTaskId, CREATED_AT, title))
                .build()
                .toByteArray());

    }

    private void produceChange(
            final String userTaskId,
            final String title) {

        produce(userTaskId, BcEvent
                .newBuilder()
                .setUserTaskUpdatedV11(reportedTask(userTaskId, CHANGED_AT, title).setUpdated(true))
                .build()
                .toByteArray());

    }

    /**
     * The save of the creation fails twice. Before, the creation was lost, and the change after it
     * created the task as if it had never had a creation: with the time of the change as its start.
     */
    @Test
    void aCreationWhichCouldNotBeStoredComesAgainBeforeTheChangeBehindIt(
            final CapturedOutput output) {

        savesToFail.set(2);

        cockpit.run(context -> {

            assertThat(context).hasNotFailed();

            produceCreation("task-1", "Assign a driver");
            produceChange("task-1", "Assign a driver, quickly");
            produceCreation("task-2", "Pay the driver");

            await()
                    .atMost(ARRIVAL)
                    .untilAsserted(() -> assertThat(savedInOrder).contains("task-2"));

            assertThat(savedInOrder).containsExactly("task-1", "task-1", "task-2");
            final var stored = storedTasks.get("task-1");
            assertThat(stored.getCreatedAt().toInstant())
                    .as("the creation came again and was stored first")
                    .isEqualTo(CREATED_AT.toInstant());
            assertThat(stored.getTitle()).containsEntry("en", "Assign a driver, quickly");
            assertThat(stored.getLatestEventAt().toInstant()).isEqualTo(CHANGED_AT.toInstant());

        });

        assertThat(LoggedErrors.of(output))
                .as("one error per attempt which failed, and nothing from the service besides")
                .hasSize(2)
                .allMatch(line -> line.contains("Handing a Kafka record over again, attempt ")
                        && line.contains("topic '" + userTaskTopic + "', partition 0, offset 0, key 'task-1'. "
                                + "The cause: org.springframework.dao.DataAccessResourceFailureException: "
                                + "MongoDB is gone for a moment"));

    }

    /**
     * MongoDB is gone before the save: reading the stored task throws. Spring Kafka's own handler
     * gave up such a record after ten attempts in a row, without a pause between them.
     */
    @Test
    void aReportWhoseReadFailedComesAgain(
            final CapturedOutput output) {

        readsToFail.set(3);

        cockpit.run(context -> {

            assertThat(context).hasNotFailed();

            produceCreation("task-1", "Assign a driver");

            await()
                    .atMost(ARRIVAL)
                    .untilAsserted(() -> assertThat(savedInOrder).containsExactly("task-1"));

        });

        assertThat(LoggedErrors.of(output))
                .as("a failed read was logged at no level above INFO before, and nobody saw why the record waited")
                .hasSize(3)
                .allMatch(line -> line.contains("Handing a Kafka record over again, attempt ")
                        && line.contains("key 'task-1'. The cause: org.springframework.dao.DataAccessResourceFailureException"));

    }

    /**
     * A report MongoDB refuses every time, like one larger than a document may be, fails the same
     * way however often it comes. Repeating it held up every record behind it for good. So it is
     * passed over with one error, which names the record and says why.
     */
    @Test
    void aReportMongoDbRefusesEveryTimeIsPassedOverAndNamed(
            final CapturedOutput output) {

        tooLarge.add("task-1");

        cockpit.run(context -> {

            assertThat(context).hasNotFailed();

            produceCreation("task-1", "Assign a driver");
            produceCreation("task-2", "Pay the driver");

            await()
                    .atMost(ARRIVAL)
                    .untilAsserted(() -> assertThat(savedInOrder).containsExactly("task-2"));

        });

        assertThat(storedTasks).doesNotContainKey("task-1");
        assertThat(LoggedErrors.of(output))
                .singleElement()
                .satisfies(line -> assertThat(line).contains(
                        "Passing over a Kafka record the cockpit cannot store: topic '"
                                + userTaskTopic
                                + "', partition 0, offset 0, key 'task-1'. Storing it fails the same way each time. "
                                + "The user task 'task-1' cannot be stored: it is larger than the 16 MB MongoDB takes "
                                + "for one document."));

    }

    /**
     * A record which is not a protobuf message is read the same way every time. Repeating it
     * would hold up every record behind it for good. So it is passed over, and the log names it.
     */
    @Test
    void aRecordWhichCannotBeReadIsPassedOverAndNamed(
            final CapturedOutput output) {

        cockpit.run(context -> {

            assertThat(context).hasNotFailed();

            // a field of 127 bytes which ends after none of them
            produce("broken", new byte[] { 0x0A, 0x7F });
            produceCreation("task-1", "Assign a driver");

            await()
                    .atMost(ARRIVAL)
                    .untilAsserted(() -> assertThat(savedInOrder).containsExactly("task-1"));

        });

        assertThat(LoggedErrors.of(output))
                .singleElement()
                .satisfies(line -> assertThat(line).contains(
                        "Passing over a Kafka record the cockpit cannot read: topic '"
                                + userTaskTopic
                                + "', partition 0, offset 0, key 'broken'"));

    }

}
