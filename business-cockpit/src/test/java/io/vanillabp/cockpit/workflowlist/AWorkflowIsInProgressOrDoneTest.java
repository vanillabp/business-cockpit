package io.vanillabp.cockpit.workflowlist;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Query;
import org.testcontainers.mongodb.MongoDBContainer;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import io.vanillabp.cockpit.commons.mongo.converters.BigDecimalReadConverter;
import io.vanillabp.cockpit.commons.mongo.converters.BigDecimalWriteConverter;
import io.vanillabp.cockpit.commons.mongo.converters.OffsetDateTimeReadConverter;
import io.vanillabp.cockpit.commons.mongo.converters.OffsetDateTimeWriteConverter;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService.RetrieveItemsMode;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.integration.test.utils.ContainerImages;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A user pages through the list of workflows from a fixed moment on. A workflow which ends after
 * that moment still counts as in progress, so the pages stay the same while the user reads them.
 * The list of the ended workflows has to agree: such a workflow is not in it yet. Otherwise it
 * shows up in both lists at once.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class AWorkflowIsInProgressOrDoneTest {

  private static final MongoDBContainer MONGODB = new MongoDBContainer(ContainerImages.MONGODB);

  private static final String DATABASE = "in-progress-or-done";

  private static final OffsetDateTime PAGING_STARTED = OffsetDateTime.of(
      2026, 11, 7, 12, 0, 0, 0, ZoneOffset.UTC);

  private static MongoClient mongoClient;

  private static MongoTemplate mongoTemplate;

  @BeforeAll
  static void startMongoDb() {

    MONGODB.start();
    mongoClient = MongoClients.create(MONGODB.getConnectionString());
    mongoTemplate = cockpitLikeTemplate(mongoClient);
    mongoTemplate.insert(List.of(
        new Document("_id", "running"),
        new Document("_id", "endedBefore")
            .append("endedAt", Date.from(PAGING_STARTED.minusHours(1).toInstant())),
        new Document("_id", "endedWhilePaging")
            .append("endedAt", Date.from(PAGING_STARTED.plusMinutes(5).toInstant()))),
        Workflow.COLLECTION_NAME);

  }

  @AfterAll
  static void stopMongoDb() {

    if (mongoClient != null) {
      mongoClient.close();
    }
    MONGODB.stop();

  }

  private static MongoTemplate cockpitLikeTemplate(
      final MongoClient mongoClient) {

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

  private static List<String> idsIn(
      final RetrieveItemsMode mode) {

    final var criteria = new WorkflowlistService().buildWorkflowlistCriteria(
        WorkflowVisibility.everyWorkflow(), PAGING_STARTED, mode, null, null);
    return mongoTemplate
        .find(new Query(criteria), Workflow.class)
        .stream()
        .map(Workflow::getId)
        .sorted()
        .toList();

  }

  @Test
  void aWorkflowWhichEndsWhileTheUserPagesIsStillInProgress() {

    assertThat(idsIn(RetrieveItemsMode.InProgress)).containsExactly("endedWhilePaging", "running");

  }

  @Test
  void theEndedWorkflowsAreThoseWhichEndedBeforeTheUserStartedToPage() {

    assertThat(idsIn(RetrieveItemsMode.Done)).containsExactly("endedBefore");

  }

  @Test
  void everyWorkflowIsInExactlyOneOfTheTwoLists() {

    final var inProgress = idsIn(RetrieveItemsMode.InProgress);
    final var done = idsIn(RetrieveItemsMode.Done);

    assertThat(inProgress).doesNotContainAnyElementsOf(done);
    assertThat(List.of(inProgress, done).stream().mapToInt(List::size).sum())
        .isEqualTo(idsIn(RetrieveItemsMode.All).size());

  }

}
