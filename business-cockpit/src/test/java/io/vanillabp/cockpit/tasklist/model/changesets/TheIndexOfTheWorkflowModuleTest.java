package io.vanillabp.cockpit.tasklist.model.changesets;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexField;
import org.springframework.data.mongodb.core.index.IndexInfo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.IndexOptions;

import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.integration.test.utils.ContainerImages;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The fifth changeset of the user tasks and of the workflows indexes the workflow module, for a
 * list which shows some workflow modules only. Each test runs on a database of its own.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class TheIndexOfTheWorkflowModuleTest {

  private static final String INDEX = "_workflowModuleIdAndEndedAt";

  private static final List<String> FIELDS = List.of("workflowModuleId", "endedAt");

  private static final org.testcontainers.mongodb.MongoDBContainer MONGODB = new org.testcontainers.mongodb.MongoDBContainer(ContainerImages.MONGODB);

  private static MongoClient mongoClient;

  private MongoTemplate mongo;

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

  @BeforeEach
  void aDatabaseOfItsOwn() {

    mongo = new MongoTemplate(mongoClient, "cockpit-"
        + UUID.randomUUID());

  }

  /** The collection and the changeset which indexes it. */
  static List<Arguments> bothCollections() {

    return List.of(
        Arguments.of(
            UserTask.COLLECTION_NAME,
            (Function<MongoTemplate, String>) mongo -> new V000005().indexTheWorkflowModuleOfAUserTask(mongo)),
        Arguments.of(
            Workflow.COLLECTION_NAME,
            (Function<MongoTemplate, String>) mongo -> new io.vanillabp.cockpit.workflowlist.model.changesets.V000005()
                .indexTheWorkflowModuleOfAWorkflow(mongo)));

  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("bothCollections")
  void theChangesetCreatesTheIndexAndItsRollbackDropsIt(
      final String collection,
      final Function<MongoTemplate, String> changeset) {

    mongo.createCollection(collection);

    final var rollback = changeset.apply(mongo);

    assertThat(indexesOf(collection)).containsEntry(INDEX, FIELDS);
    mongo.getDb().runCommand(Document.parse(rollback));
    assertThat(indexesOf(collection)).doesNotContainKey(INDEX);

  }

  /**
   * MongoDB refuses a second index on the same fields under another name. So a database which has
   * one already keeps it, and the changeset has nothing to undo.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("bothCollections")
  void anIndexOnTheSameFieldsUnderAnotherNameIsKept(
      final String collection,
      final Function<MongoTemplate, String> changeset) {

    mongo.getCollection(collection).createIndex(
        new Document("workflowModuleId", 1).append("endedAt", 1),
        new IndexOptions().name("workflow_workflowModuleIdAndEndedAt"));

    final var rollback = changeset.apply(mongo);

    assertThat(rollback).isNull();
    assertThat(indexesOf(collection))
        .containsEntry("workflow_workflowModuleIdAndEndedAt", FIELDS)
        .doesNotContainKey(INDEX);

  }

  private Map<String, List<String>> indexesOf(
      final String collection) {

    final var result = new HashMap<String, List<String>>();
    for (final IndexInfo index : mongo.indexOps(collection).getIndexInfo()) {
      result.put(index.getName(), index
          .getIndexFields()
          .stream()
          .map(IndexField::getKey)
          .toList());
    }
    return result;

  }

}
