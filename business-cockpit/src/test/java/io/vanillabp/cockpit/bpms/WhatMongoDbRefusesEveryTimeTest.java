package io.vanillabp.cockpit.bpms;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bson.BsonMaximumSizeExceededException;
import org.bson.Document;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.validation.Validator;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.mongodb.MongoDBContainer;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import io.vanillabp.cockpit.config.startup.CockpitConfiguration;
import io.vanillabp.cockpit.config.startup.MapKeyDotReplacement;
import io.vanillabp.cockpit.mongo.converters.BigDecimalReadConverter;
import io.vanillabp.cockpit.mongo.converters.BigDecimalWriteConverter;
import io.vanillabp.cockpit.mongo.converters.OffsetDateTimeReadConverter;
import io.vanillabp.cockpit.mongo.converters.OffsetDateTimeWriteConverter;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.integration.test.utils.ContainerImages;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Which failed saves {@link OutcomeOfStoring} treats as failing every time. Measured against a
 * real MongoDB and the repository Spring Data builds.
 * <p>
 * A double cannot answer this. What matters is the exception which arrives at the service: whether
 * Spring translates it, and what it wraps. Each test stores one user task through the real
 * {@link UserTaskService}, so the outcome is the one a REST call or a Kafka record gets.
 * <p>
 * All failures which fail every time are about the document alone, so the same report fails
 * the same way however often it comes. The one failure shown which goes away is a report stored in
 * between.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class WhatMongoDbRefusesEveryTimeTest {

  private static final TypeReference<Map<String, Object>> A_JSON_OBJECT = new TypeReference<>() {
  };

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

    final var factory = new SimpleMongoClientDatabaseFactory(mongoClient, "cockpit-"
        + UUID.randomUUID());
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
        .isEqualTo(
            "The user task 'task-1' cannot be stored: it is larger than the 16 MB MongoDB takes for one document.");

  }

  @Test
  void aTaskWhichBreaksTheRulesOfTheCollectionFailsEveryTime() {

    final var mongoTemplate = cockpitLikeTemplate();
    // a rule an operator could set. The cockpit sets none itself
    mongoTemplate.createCollection(
        UserTask.COLLECTION_NAME,
        CollectionOptions.empty()
            .validator(Validator.document(new Document("businessId", new Document("$exists", true)))));
    final var service = serviceOn(mongoTemplate);

    final var outcome = service.reportCreatedUserTask("task-1", CREATED_AT, () -> reportedTask("task-1", null));

    assertThat(outcome.isUpToDate()).isFalse();
    assertThat(outcome.failure())
        .as("Spring wraps the MongoWriteException of the driver")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(outcome.failsEveryTime()).isTrue();
    assertThat(outcome.reason())
        .isEqualTo(
            "The user task 'task-1' cannot be stored: MongoDB refuses it, because it breaks the validation rules of the collection.");

  }

  /**
   * An index over two fields which both hold a list, created while the collection was empty.
   * MongoDB then refuses every task which holds both fields, and it refuses it again however often
   * it comes.
   */
  @Test
  void aTaskWhichAnIndexOverTwoListsCannotTakeFailsEveryTime() {

    final var mongoTemplate = cockpitLikeTemplate();
    mongoTemplate.createCollection(UserTask.COLLECTION_NAME);
    mongoTemplate
        .indexOps(UserTask.COLLECTION_NAME)
        .createIndex(new Index()
            .on("candidateUsers.id", Sort.Direction.ASC)
            .on("candidateGroups.id", Sort.Direction.ASC));
    final var service = serviceOn(mongoTemplate);
    final var task = reportedTask("task-1", null);
    task.setCandidateUsers(new ArrayList<>());
    task.setCandidateGroups(new ArrayList<>());

    final var outcome = service.reportCreatedUserTask("task-1", CREATED_AT, () -> task);

    assertThat(outcome.isUpToDate()).isFalse();
    assertThat(outcome.failsEveryTime()).isTrue();
    assertThat(outcome.reason()).isEqualTo(
        "The user task 'task-1' cannot be stored: an index of the collection covers two fields which both hold a list. "
            + "Drop that index, its name starts with '_sort_'.");

  }

  /**
   * MongoDB keeps a decimal number in 34 digits. A number with more, as {@code new BigDecimal(0.1)}
   * gives it, cannot be converted, and it cannot be converted when it comes again either. A report
   * over Kafka carries every number as such a decimal.
   */
  @Test
  void aNumberWithMoreDigitsThanMongoDbKeepsFailsEveryTime() {

    final var service = serviceOn(cockpitLikeTemplate());
    final var digits = "0.1000000000000000055511151231257827021181583404541015625";

    final var outcome = service.reportCreatedUserTask(
        "task-1",
        CREATED_AT,
        () -> reportedTask("task-1", Map.of("amount", new BigDecimal(digits))));

    assertThat(outcome.isUpToDate()).isFalse();
    assertThat(outcome.failure())
        .as("Spring wraps what the converter throws")
        .isInstanceOf(ConversionFailedException.class)
        .hasRootCauseInstanceOf(NumberFormatException.class);
    assertThat(outcome.failsEveryTime()).isTrue();
    assertThat(outcome.reason())
        .isEqualTo("The user task 'task-1' cannot be stored: it holds a value which MongoDB cannot store, "
            + "like a decimal number with more than 34 digits or a whole number beyond the range of a long.")
        .as("the reason never repeats a value of the report")
        .doesNotContain("1000000000000000055511151231257827021181583404541015625");

  }

  /** A decimal number whose exponent MongoDB cannot keep fails the same way. */
  @Test
  void aNumberWithAnExponentMongoDbCannotTakeFailsEveryTime() {

    final var service = serviceOn(cockpitLikeTemplate());

    final var outcome = service.reportCreatedUserTask(
        "task-1",
        CREATED_AT,
        () -> reportedTask("task-1", Map.of("amount", new BigDecimal("1E+7000"))));

    assertThat(outcome.isUpToDate()).isFalse();
    assertThat(outcome.failure())
        .isInstanceOf(ConversionFailedException.class)
        .hasRootCauseInstanceOf(NumberFormatException.class);
    assertThat(outcome.failsEveryTime()).isTrue();
    assertThat(outcome.reason()).doesNotContain("7000");

  }

  /**
   * What a report over REST carries: Jackson reads a decimal number of the JSON body as a double,
   * which MongoDB stores rounded, and a whole number beyond the range of a long as a
   * {@link BigInteger}. The driver of MongoDB cannot store a {@code BigInteger}, so such a report
   * fails every time.
   */
  @Test
  void aWholeNumberOfAJsonBodyBeyondTheRangeOfALongFailsEveryTime() {

    final var mongoTemplate = cockpitLikeTemplate();
    final var service = serviceOn(mongoTemplate);
    final Map<String, Object> decimal = JsonMapper
        .builder()
        .build()
        .readValue("{\"amount\": 0.1000000000000000055511151231257827021181583404541015625}", A_JSON_OBJECT);
    final Map<String, Object> whole = JsonMapper
        .builder()
        .build()
        .readValue("{\"amount\": 10000000000000000000000000000000000}", A_JSON_OBJECT);

    final var decimalOutcome = service.reportCreatedUserTask("task-1", CREATED_AT,
        () -> reportedTask("task-1", decimal));
    final var wholeOutcome = service.reportCreatedUserTask("task-2", CREATED_AT, () -> reportedTask("task-2", whole));

    assertThat(decimal.get("amount")).isInstanceOf(Double.class);
    assertThat(decimalOutcome.isUpToDate()).isTrue();
    assertThat(whole.get("amount")).isInstanceOf(BigInteger.class);
    assertThat(wholeOutcome.isUpToDate()).isFalse();
    assertThat(wholeOutcome.failure())
        .as("the driver has no codec for a BigInteger, whatever its size")
        .isInstanceOf(CodecConfigurationException.class);
    assertThat(wholeOutcome.failsEveryTime()).isTrue();
    assertThat(wholeOutcome.reason()).doesNotContain("10000000000000000000000000000000000");

  }

  /** The positive twin of the tests above: a decimal number of 34 digits is stored as it is. */
  @Test
  void aNumberOf34DigitsIsStored() {

    final var mongoTemplate = cockpitLikeTemplate();
    final var service = serviceOn(mongoTemplate);
    final var digits = new BigDecimal("0.1000000000000000055511151231257827");

    final var outcome = service.reportCreatedUserTask(
        "task-1",
        CREATED_AT,
        () -> reportedTask("task-1", Map.of("amount", digits)));

    assertThat(outcome.isUpToDate()).isTrue();
    assertThat(service.getUserTask("task-1").getDetails().get("amount")).hasToString(digits.toPlainString());

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
        .contains("set '"
            + CockpitConfiguration.MONGODB_MAP_KEY_DOT_REPLACEMENT
            + "' in the cockpit")
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
