package io.vanillabp.cockpit.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.testcontainers.mongodb.MongoDBContainer;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import io.vanillabp.integration.test.utils.ContainerImages;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What MongoDB does with one index over two list fields of the same document, like the candidate
 * users and the candidate groups of a user task. MongoDB indexes at most one list field per
 * document in such an index. This test measures when it refuses: when the index is created, or
 * when a document is written afterwards. With MongoDB 8.2, the image the tests of this repository
 * use, it creates the index on an empty collection and then refuses every document which holds
 * both fields, even as empty lists. On a collection which holds such a document, it refuses the
 * index. The cockpit refuses a sort which would create such an
 * index, and it treats the refusal of a write as one which fails every time.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class AnIndexOverTwoListFieldsTest {

  /** MongoDB's code for a document with two list fields in one index. */
  private static final int CANNOT_INDEX_PARALLEL_ARRAYS = 171;

  private static final String COLLECTION = "usertask";

  private static final Document PERSON = new Document("id", "alice").append("sort", "Alice");

  private static final Document GROUP = new Document("id", "CLERK");

  private static final MongoDBContainer MONGODB = new MongoDBContainer(ContainerImages.MONGODB);

  private static MongoClient mongoClient;

  private MongoTemplate mongoTemplate;

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

    mongoTemplate = new MongoTemplate(mongoClient, "cockpit-"
        + UUID.randomUUID());
    mongoTemplate.createCollection(COLLECTION);

  }

  private void createTheIndex() {

    mongoTemplate
        .indexOps(COLLECTION)
        .createIndex(new Index()
            .on("candidateUsers.sort", Sort.Direction.ASC)
            .on("candidateGroups.id", Sort.Direction.ASC)
            .named(SortIndexes.INDEX_PREFIX
                + "candidateUsers.sort,candidateGroups.id"));

  }

  private static Document task(
      final List<Document> candidateUsers,
      final List<Document> candidateGroups) {

    return new Document("_id", UUID.randomUUID().toString())
        .append("candidateUsers", candidateUsers)
        .append("candidateGroups", candidateGroups);

  }

  /**
   * @return The code MongoDB refused the write with, or {@code 0} if it took it
   */
  private int write(
      final Document task) {

    try {
      mongoTemplate.getCollection(COLLECTION).insertOne(task);
      return 0;
    } catch (MongoException e) {
      return e.getCode();
    }

  }

  /**
   * @return The code MongoDB refused the index with, or {@code 0} if it created it
   */
  private int index() {

    try {
      createTheIndex();
      return 0;
    } catch (RuntimeException e) {
      for (Throwable cause = e; cause != null; cause = cause.getCause()) {
        if (cause instanceof MongoException mongo) {
          return mongo.getCode();
        }
      }
      throw e;
    }

  }

  /** Measured with MongoDB 8.2: two empty lists count as two lists. */
  @Test
  void onAnEmptyCollectionTheIndexIsCreatedAndATaskWithTwoEmptyListsIsRefused() {

    assertThat(index()).isZero();
    assertThat(write(task(List.of(), List.of()))).isEqualTo(CANNOT_INDEX_PARALLEL_ARRAYS);

  }

  @Test
  void onAnEmptyCollectionTheIndexIsCreatedAndATaskWithTwoFilledListsIsRefused() {

    assertThat(index()).isZero();
    assertThat(write(task(List.of(PERSON), List.of(GROUP)))).isEqualTo(CANNOT_INDEX_PARALLEL_ARRAYS);

  }

  @Test
  void onAFilledCollectionTheIndexIsRefused() {

    assertThat(write(task(List.of(PERSON), List.of(GROUP)))).isZero();
    assertThat(index()).isEqualTo(CANNOT_INDEX_PARALLEL_ARRAYS);

  }

  /** Two paths in the same list field, like a column of persons sends them, are no problem. */
  @Test
  void twoPathsInOneListFieldAreIndexed() {

    mongoTemplate
        .indexOps(COLLECTION)
        .createIndex(new Index()
            .on("candidateUsers.sort", Sort.Direction.ASC)
            .on("candidateUsers.id", Sort.Direction.ASC)
            .on("dueDate", Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC));

    assertThat(write(task(List.of(PERSON), List.of(GROUP)))).isZero();

  }

}
