package io.vanillabp.cockpit.util.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import io.vanillabp.cockpit.commons.mongo.MongoDbProperties;
import io.vanillabp.cockpit.commons.mongo.changestreams.ChangeStreamUtils;
import io.vanillabp.cockpit.commons.mongo.converters.BigDecimalReadConverter;
import io.vanillabp.cockpit.commons.mongo.converters.BigDecimalWriteConverter;
import io.vanillabp.cockpit.commons.mongo.converters.OffsetDateTimeReadConverter;
import io.vanillabp.cockpit.commons.mongo.converters.OffsetDateTimeWriteConverter;
import io.vanillabp.cockpit.tasklist.UserTaskChangedNotification;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.util.events.NotificationEvent.Type;
import io.vanillabp.cockpit.workflowlist.WorkflowChangedNotification;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.integration.test.utils.ContainerImages;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.messaging.DefaultMessageListenerContainer;
import org.springframework.data.mongodb.core.messaging.MessageListener;
import org.springframework.data.mongodb.core.messaging.Subscription;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * Counts the notifications a real MongoDB change stream produces for the three ways the cockpit
 * writes a document, and so decides whether an open browser learns about a change at all.
 *
 * <p>The three writes are not interchangeable, and that is the point of this test. An insert and a
 * replace carry the new document in the change event whatever the stream was subscribed with; an
 * update carries only the key of the document unless the stream asks for a lookup, which this one
 * deliberately does not. Spring Data decides between replace and update by what it has to send:
 * {@code save()} of a versioned document is a {@code replaceOne}, while
 * {@code UserTaskService.unclaimTask} sends a {@code $unset} and is an {@code updateOne}. So giving
 * a task back is the write which arrives without a document, and a notification built from that
 * document is the one which never reaches the browser.
 *
 * <p>The test brings its own MongoDB and wires the change stream by hand, because the cockpit's
 * application context needs a lot more than a database and would hide which of the three writes was
 * lost.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class ChangeStreamNotificationsTest {

    private static final String DATABASE = "business-cockpit";

    /** Generous, because a change stream is polled and the poll is answered at most once a second. */
    private static final Duration ARRIVAL = Duration.ofSeconds(30);

    private static final MongoDBContainer MONGODB =
            new MongoDBContainer(ContainerImages.MONGODB).withReplicaSet();

    private static MongoClient mongoClient;
    private static MongoTemplate mongoTemplate;
    private static DefaultMessageListenerContainer listenerContainer;

    @BeforeAll
    static void startMongoDb() {

        MONGODB.start();
        mongoClient = MongoClients.create(MONGODB.getReplicaSetUrl(DATABASE));
        mongoTemplate = cockpitLikeTemplate();
        // a change stream is opened on a collection, so the collections exist before it starts.
        // In the running cockpit the changesets create them, see ChangeStreamUtils
        mongoTemplate.createCollection(UserTask.COLLECTION_NAME);
        mongoTemplate.createCollection(Workflow.COLLECTION_NAME);

        final var executor = new SimpleAsyncTaskExecutor("change-stream-test-");
        executor.setDaemon(true);
        listenerContainer = new DefaultMessageListenerContainer(mongoTemplate, executor);
        listenerContainer.start();

    }

    @AfterAll
    static void stopMongoDb() {

        if (listenerContainer != null) {
            listenerContainer.stop();
        }
        if (mongoClient != null) {
            mongoClient.close();
        }
        MONGODB.stop();

    }

    /**
     * A template with the converters of {@code MongoDbConfiguration}, so a document written here
     * looks like one the cockpit wrote. Without them an {@code OffsetDateTime} cannot be stored,
     * and every timestamp of a user task is one.
     */
    private static MongoTemplate cockpitLikeTemplate() {

        final var factory = new SimpleMongoClientDatabaseFactory(mongoClient, DATABASE);
        final var conversions = new MongoCustomConversions(List.of(
                new OffsetDateTimeReadConverter(),
                new OffsetDateTimeWriteConverter(),
                new BigDecimalReadConverter(),
                new BigDecimalWriteConverter()));
        final var mappingContext = new MongoMappingContext();
        mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        mappingContext.afterPropertiesSet();
        final var converter = new MappingMongoConverter(new DefaultDbRefResolver(factory), mappingContext);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();
        return new MongoTemplate(factory, converter);

    }

    /**
     * Records what the change-stream wrapper logs, so a test can assert what a swallowed listener
     * failure tells the next reader. The messages are kept formatted, which is what a log file
     * shows.
     */
    private static class RecordingLogger extends LegacyAbstractLogger {

        private final Queue<String> warnings = new ConcurrentLinkedQueue<>();

        Queue<String> warnings() {
            return warnings;
        }

        @Override
        protected void handleNormalizedLoggingCall(
                final Level level,
                final Marker marker,
                final String messagePattern,
                final Object[] arguments,
                final Throwable throwable) {

            if (level == Level.WARN) {
                warnings.add(MessageFormatter.basicArrayFormat(messagePattern, arguments));
            }

        }

        @Override
        protected String getFullyQualifiedCallerName() {
            return null;
        }

        @Override
        public boolean isTraceEnabled() {
            return false;
        }

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public boolean isInfoEnabled() {
            return false;
        }

        @Override
        public boolean isWarnEnabled() {
            return true;
        }

        @Override
        public boolean isErrorEnabled() {
            return true;
        }

    }

    /**
     * A {@link ChangeStreamUtils} on the test's database, subscribing the way the two services do:
     * every operation type and no lookup of the full document.
     */
    private static ChangeStreamUtils changeStreamUtils(
            final Logger logger) {

        final var changeStreamUtils = new ChangeStreamUtils();
        ReflectionTestUtils.setField(changeStreamUtils, "logger", logger);
        ReflectionTestUtils.setField(changeStreamUtils, "properties", new MongoDbProperties());
        ReflectionTestUtils.setField(changeStreamUtils, "messageListenerContainer", listenerContainer);
        return changeStreamUtils;

    }

    private static <T> Subscription subscribed(
            final ChangeStreamUtils changeStreamUtils,
            final Class<T> entityClass,
            final MessageListener<ChangeStreamDocument<Document>, T> listener) {

        final var subscription = changeStreamUtils.subscribe(entityClass, listener);
        try {
            assertThat(subscription.await(ARRIVAL))
                    .as("change stream of '%s' active", entityClass.getSimpleName())
                    .isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while the change stream started", e);
        }
        return subscription;

    }

    private static UserTask userTask(
            final String id) {

        final var userTask = new UserTask();
        userTask.setId(id);
        userTask.setWorkflowModuleId("test-module");
        userTask.setBpmnProcessId("taxi-ride");
        userTask.setCreatedAt(OffsetDateTime.now());
        userTask.setUpdatedAt(OffsetDateTime.now());
        return userTask;

    }

    private static Workflow workflow(
            final String id) {

        final var workflow = new Workflow();
        workflow.setId(id);
        workflow.setWorkflowModuleId("test-module");
        workflow.setBpmnProcessId("taxi-ride");
        workflow.setCreatedAt(OffsetDateTime.now());
        workflow.setUpdatedAt(OffsetDateTime.now());
        return workflow;

    }

    private static Person person(
            final String id) {

        final var person = new Person();
        person.setId(id);
        return person;

    }

    @Test
    void allThreeWritesOfAUserTaskBecomeANotification() {

        final var logger = new RecordingLogger();
        final var changeStreams = changeStreamUtils(logger);
        final var notifications = new ConcurrentLinkedQueue<UserTaskChangedNotification>();
        final var subscription = subscribed(
                changeStreams,
                UserTask.class,
                message -> notifications.add(UserTaskChangedNotification.build(message)));

        final var userTaskId = "task-" + UUID.randomUUID();
        try {
            // the workflow system reports a new task: an insert
            mongoTemplate.insert(userTask(userTaskId));

            // somebody claims it. The document is versioned, so Spring Data sends a replaceOne
            final var claimed = mongoTemplate.findById(userTaskId, UserTask.class);
            claimed.setAssignee(person("martin"));
            mongoTemplate.save(claimed);

            // the same person gives the task back, the way UserTaskService.unclaimTask does it:
            // an updateOne, and the only one of the three writes without a document in the event
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(userTaskId)),
                    new Update()
                            .unset("assignee")
                            .set("updatedAt", OffsetDateTime.now())
                            .set("updatedBy", "martin"),
                    UserTask.class);

            await()
                    .atMost(ARRIVAL)
                    .untilAsserted(() -> assertThat(notifications)
                            .as("notifications built from the change stream, swallowed warnings: %s",
                                    logger.warnings())
                            .extracting(NotificationEvent::getType)
                            .containsExactly(Type.INSERT, Type.UPDATE, Type.UPDATE));
            assertThat(notifications)
                    .extracting(UserTaskChangedNotification::getUserTaskId)
                    .containsOnly(userTaskId);
            assertThat(logger.warnings()).isEmpty();
        } finally {
            changeStreams.unsubscribe(subscription);
        }

    }

    /**
     * The same three writes on the workflow side. A workflow notification is built without reading
     * the document, so this is the state the user-task side is brought to.
     */
    @Test
    void allThreeWritesOfAWorkflowBecomeANotification() {

        final var logger = new RecordingLogger();
        final var changeStreams = changeStreamUtils(logger);
        final var notifications = new ConcurrentLinkedQueue<WorkflowChangedNotification>();
        final var subscription = subscribed(
                changeStreams,
                Workflow.class,
                message -> notifications.add(WorkflowChangedNotification.build(message)));

        final var workflowId = "workflow-" + UUID.randomUUID();
        try {
            mongoTemplate.insert(workflow(workflowId));

            final var reported = mongoTemplate.findById(workflowId, Workflow.class);
            reported.setTitle(Map.of("en", "Ride to the airport"));
            mongoTemplate.save(reported);

            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(workflowId)),
                    new Update()
                            .unset("title")
                            .set("updatedAt", OffsetDateTime.now()),
                    Workflow.class);

            await()
                    .atMost(ARRIVAL)
                    .untilAsserted(() -> assertThat(notifications)
                            .as("notifications built from the change stream, swallowed warnings: %s",
                                    logger.warnings())
                            .extracting(NotificationEvent::getType)
                            .containsExactly(Type.INSERT, Type.UPDATE, Type.UPDATE));
            assertThat(logger.warnings()).isEmpty();
        } finally {
            changeStreams.unsubscribe(subscription);
        }

    }

    /**
     * A listener which fails must not take the stream down, and what it logs has to name the one
     * change which was lost. Without the document key and the operation type the next reader knows
     * that something failed and nothing else.
     */
    @Test
    void aFailingListenerIsReportedWithTheDocumentAndTheOperation() {

        final var logger = new RecordingLogger();
        final var changeStreams = changeStreamUtils(logger);
        final var subscription = subscribed(
                changeStreams,
                UserTask.class,
                message -> {
                    throw new IllegalStateException("on purpose");
                });

        final var userTaskId = "task-" + UUID.randomUUID();
        try {
            mongoTemplate.insert(userTask(userTaskId));

            await()
                    .atMost(ARRIVAL)
                    .untilAsserted(() -> assertThat(logger.warnings()).isNotEmpty());
            assertThat(logger.warnings())
                    .allSatisfy(warning -> assertThat(warning)
                            .contains(UserTask.COLLECTION_NAME)
                            .contains(userTaskId)
                            .contains("insert"));
        } finally {
            changeStreams.unsubscribe(subscription);
        }

    }

}
