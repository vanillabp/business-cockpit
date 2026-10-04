package io.vanillabp.cockpit.bpms;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.vanillabp.cockpit.commons.mongo.converters.BigDecimalReadConverter;
import io.vanillabp.cockpit.commons.mongo.converters.BigDecimalWriteConverter;
import io.vanillabp.cockpit.commons.mongo.converters.OffsetDateTimeReadConverter;
import io.vanillabp.cockpit.commons.mongo.converters.OffsetDateTimeWriteConverter;
import io.vanillabp.cockpit.config.startup.CockpitConfiguration;
import io.vanillabp.cockpit.config.startup.MapKeyDotReplacement;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.integration.test.utils.ContainerImages;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bson.BsonMaximumSizeExceededException;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.validation.Validator;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * Which failures of a save {@link OutcomeOfStoring} takes as failing every time, measured against
 * a real MongoDB and the repository Spring Data builds.
 * <p>
 * A double cannot answer this. What matters is the exception which arrives at the service: whether
 * Spring translates it, and what it wraps. Each test stores one user task through the real
 * {@link UserTaskService}, so the outcome is the one a REST call or a Kafka record gets.
 * <p>
 * All three failures which fail every time are about the document alone, so the same report fails
 * the same way however often it comes. The one failure shown which goes away is a report stored in
 * between.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class WhatMongoDbRefusesEveryTimeTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-10-04T08:00:00Z");

    private static final MongoDBContainer MONGODB = new MongoDBContainer(ContainerImages.MONGODB);

    private static MongoClient mongoClient;

    @BeforeAll
    static void startMongoDb() {

        MONGODB.start();
        mongoClient = MongoClients.create(MONGODB.getConnectionString());

    }

    @AfterAll
    static void stopMongoDb() {

        if (mongoClient != null) {
            mongoClient.close();
        }
        MONGODB.stop();

    }

    /**
     * A template with the converters of {@code MongoDbConfiguration}, in a database of its own, so
     * that a rule one test sets on a collection does not reach another test.
     */
    private static MongoTemplate cockpitLikeTemplate() {

        return cockpitLikeTemplate(new MockEnvironment());

    }

    /**
     * @param environment The configuration the cockpit's MongoDB configuration reads the
     *        replacement of a dot from
     */
    private static MongoTemplate cockpitLikeTemplate(
            final MockEnvironment environment) {

        final var factory = new SimpleMongoClientDatabaseFactory(mongoClient, "cockpit-" + UUID.randomUUID());
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
        // what MongoDbConfiguration does with the converter before it builds the template
        MapKeyDotReplacement.applyTo(environment, converter);
        return new MongoTemplate(factory, converter);

    }

    private static UserTaskService serviceOn(
            final MongoTemplate mongoTemplate) {

        final var service = new UserTaskService();
        ReflectionTestUtils.setField(
                service,
                "userTasks",
                new MongoRepositoryFactory(mongoTemplate).getRepository(UserTaskRepository.class));
        ReflectionTestUtils.setField(service, "logger", LoggerFactory.getLogger(UserTaskService.class));
        return service;

    }

    private static UserTask reportedTask(
            final String userTaskId,
            final Map<String, Object> details) {

        final var task = new UserTask();
        task.setId(userTaskId);
        task.setCreatedAt(CREATED_AT);
        task.setWorkflowModuleId("taxi-ride");
        task.setBpmnProcessId("ride");
        task.setTitle(Map.of("en", "Assign a driver"));
        task.setDetails(details);
        return task;

    }

    @Test
    void aTaskLargerThanMongoDbTakesFailsEveryTime() {

        final var service = serviceOn(cockpitLikeTemplate());
        // 17 MB of business data, one MB more than a document may have
        final var tooLarge = reportedTask("task-1", Map.of("scan", "x".repeat(17 * 1024 * 1024)));

        final var outcome = service.reportCreatedUserTask("task-1", CREATED_AT, () -> tooLarge);

        assertThat(outcome.isUpToDate()).isFalse();
        assertThat(NestedExceptionUtils.getMostSpecificCause(outcome.failure()))
                .as("the driver throws it before it sends anything, and Spring does not translate it")
                .isInstanceOf(BsonMaximumSizeExceededException.class);
        assertThat(outcome.failsEveryTime()).isTrue();
        assertThat(outcome.reason())
                .isEqualTo("The user task 'task-1' cannot be stored: it is larger than the 16 MB MongoDB takes for one document.");

    }

    @Test
    void aTaskWhichBreaksTheRulesOfTheCollectionFailsEveryTime() {

        final var mongoTemplate = cockpitLikeTemplate();
        // a rule an operator could set. The cockpit sets none itself
        mongoTemplate.createCollection(
                UserTask.COLLECTION_NAME,
                CollectionOptions.empty().validator(Validator.document(new Document("businessId", new Document("$exists", true)))));
        final var service = serviceOn(mongoTemplate);

        final var outcome = service.reportCreatedUserTask("task-1", CREATED_AT, () -> reportedTask("task-1", null));

        assertThat(outcome.isUpToDate()).isFalse();
        assertThat(outcome.failure())
                .as("Spring wraps the MongoWriteException of the driver")
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(outcome.failsEveryTime()).isTrue();
        assertThat(outcome.reason())
                .isEqualTo("The user task 'task-1' cannot be stored: MongoDB refuses it, because it breaks the validation rules of the collection.");

    }

    /** The positive twin of the two above: a task of the same kind is stored. */
    @Test
    void aTaskWithinTheRulesIsStored() {

        final var mongoTemplate = cockpitLikeTemplate();
        final var service = serviceOn(mongoTemplate);

        final var outcome = service.reportCreatedUserTask(
                "task-1",
                CREATED_AT,
                () -> reportedTask("task-1", Map.of("scan", "x".repeat(1024 * 1024))));

        assertThat(outcome.isUpToDate()).isTrue();
        assertThat(mongoTemplate.findById("task-1", UserTask.class)).isNotNull();

    }

    /**
     * Another report about the same task was stored in between. This is the most common failure
     * which is not about MongoDB being gone, and the same report goes through when it comes again.
     */
    @Test
    void aTaskStoredByAnotherReportInBetweenFailsForNow() {

        final var mongoTemplate = cockpitLikeTemplate();
        final var service = serviceOn(mongoTemplate);
        service.reportCreatedUserTask("task-1", CREATED_AT, () -> reportedTask("task-1", null));
        final var readBeforeTheOtherReport = service.getUserTask("task-1");
        service.reportChangedUserTask("task-1", CREATED_AT.plusMinutes(1), () -> null, task -> task.setComment("first"));

        final var outcome = service.reportChangedUserTask(
                "task-1",
                CREATED_AT.plusMinutes(2),
                () -> null,
                task -> ReflectionTestUtils.setField(task, "version", readBeforeTheOtherReport.getVersion()));

        assertThat(outcome.isUpToDate()).isFalse();
        assertThat(outcome.failure()).isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(outcome.failsEveryTime()).isFalse();

    }

    /**
     * Spring Data refuses a key with a dot in it, because the cockpit configures no replacement for
     * the dot. Business data is a map the workflow module fills, so such a key can come in any
     * report.
     */
    @Test
    void aTaskWhoseBusinessDataHasAKeyWithADotFailsEveryTime() {

        final var service = serviceOn(cockpitLikeTemplate());

        final var outcome = service.reportCreatedUserTask(
                "task-1",
                CREATED_AT,
                () -> reportedTask("task-1", Map.of("order.id", "4711")));

        assertThat(outcome.isUpToDate()).isFalse();
        assertThat(outcome.failure()).isInstanceOf(MappingException.class);
        assertThat(outcome.failsEveryTime()).isTrue();
        assertThat(outcome.reason())
                .startsWith("The user task 'task-1' cannot be stored: it does not fit the form MongoDB stores it in. "
                        + "Map key order.id contains dots")
                .as("the reason names the property which lets such a key be stored, and what it costs")
                .contains("set '" + CockpitConfiguration.MONGODB_MAP_KEY_DOT_REPLACEMENT + "' in the cockpit")
                .contains("Search and sorting then find the key only by its stored form, 'order~id'")
                .contains("a key which holds the replacement already comes back with a dot in its place.")
                .doesNotContain("\n");

    }

    /**
     * The positive twin of the test above. With a replacement configured the dot is stored as the
     * replacement and comes back as a dot, also in a nested map.
     */
    @Test
    void aTaskWhoseBusinessDataHasAKeyWithADotIsStoredWhereAReplacementIsConfigured() {

        final var mongoTemplate = cockpitLikeTemplate(new MockEnvironment()
                .withProperty(CockpitConfiguration.MONGODB_MAP_KEY_DOT_REPLACEMENT, "~"));
        final var service = serviceOn(mongoTemplate);

        final var outcome = service.reportCreatedUserTask(
                "task-1",
                CREATED_AT,
                () -> reportedTask("task-1", Map.of(
                        "order.id", "4711",
                        "customer", Map.of("address.city", "Vienna"))));

        assertThat(outcome.isUpToDate()).isTrue();
        assertThat(service.getUserTask("task-1").getDetails())
                .isEqualTo(Map.of(
                        "order.id", "4711",
                        "customer", Map.of("address.city", "Vienna")));
        final var stored = mongoTemplate
                .getCollection(UserTask.COLLECTION_NAME)
                .find(new Document("_id", "task-1"))
                .first()
                .get("details", Document.class);
        assertThat(stored)
                .as("MongoDB holds the stored form, which is what search and sorting see")
                .containsEntry("order~id", "4711")
                .doesNotContainKey("order.id");
        assertThat(stored.get("customer", Document.class)).containsEntry("address~city", "Vienna");

    }

    /** What the cost named in the reason means: a key which holds the replacement comes back with a dot. */
    @Test
    void aKeyWhichHoldsTheReplacementAlreadyComesBackWithADot() {

        final var service = serviceOn(cockpitLikeTemplate(new MockEnvironment()
                .withProperty(CockpitConfiguration.MONGODB_MAP_KEY_DOT_REPLACEMENT, "~")));

        service.reportCreatedUserTask("task-1", CREATED_AT, () -> reportedTask("task-1", Map.of("size~large", "XL")));

        assertThat(service.getUserTask("task-1").getDetails()).isEqualTo(Map.of("size.large", "XL"));

    }

}
