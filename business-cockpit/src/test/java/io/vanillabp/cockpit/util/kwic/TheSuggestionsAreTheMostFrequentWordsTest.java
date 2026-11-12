package io.vanillabp.cockpit.util.kwic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.mongodb.MongoDBContainer;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.UserTaskVisibility;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.util.ListPaths;
import io.vanillabp.cockpit.workflowlist.WorkflowVisibility;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.integration.test.utils.ContainerImages;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The suggestions for a search field are the words found most often, the most frequent first.
 * There are more words to suggest than fit into the answer, so the answer has to leave the rare
 * ones out. Measured against a real MongoDB, because the order comes from the database.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class TheSuggestionsAreTheMostFrequentWordsTest {

  private static final String FULL_TEXT = "detailsFulltextSearch";

  /** Found in every row, and in none of the words the tests ask about. */
  private static final String OTHER_WORD = "taxi";

  /** Three words found equally often, more often than any other. Not in alphabetical order here. */
  private static final List<String> WORDS_FOUND_EQUALLY_OFTEN = List.of("cordial", "accord", "border");

  private static final int HOW_OFTEN_THE_EQUAL_WORDS_ARE_FOUND = 40;

  /** Words named {@code order01} to {@code order30}. Each is found as often as its number says. */
  private static final int NUMBERED_WORDS = 30;

  private static final MongoDBContainer MONGODB = new MongoDBContainer(ContainerImages.MONGODB);

  private static MongoClient mongoClient;

  private static MongoTemplate mongoTemplate;

  @BeforeAll
  static void startMongoDbWithManyWords() {

    MONGODB.start();
    mongoClient = MongoClients.create(MONGODB.getConnectionString());
    mongoTemplate = new MongoTemplate(mongoClient, "most-frequent-words");
    final var rows = new ArrayList<Document>();
    WORDS_FOUND_EQUALLY_OFTEN.forEach(word -> addRows(rows, word, HOW_OFTEN_THE_EQUAL_WORDS_ARE_FOUND));
    for (int number = 1; number <= NUMBERED_WORDS; ++number) {
      addRows(rows, numberedWord(number), number);
    }
    mongoTemplate.insert(rows, UserTask.COLLECTION_NAME);
    mongoTemplate.insert(rows, Workflow.COLLECTION_NAME);

  }

  @AfterAll
  static void stopMongoDb() {

    if (mongoClient != null) {
      mongoClient.close();
    }
    MONGODB.stop();

  }

  private static void addRows(
      final List<Document> rows,
      final String word,
      final int howMany) {

    for (int row = 0; row < howMany; ++row) {
      rows.add(new Document("_id", word
          + "-"
          + row)
          .append(FULL_TEXT, OTHER_WORD
              + " "
              + word));
    }

  }

  private static String numberedWord(
      final int number) {

    return "order%02d".formatted(number);

  }

  /**
   * The three words found equally often in alphabetical order, then the numbered words from the
   * most frequent down, until there are 21.
   */
  private static List<KwicResult> theTwentyOneMostFrequentWords() {

    final var expected = new ArrayList<KwicResult>();
    WORDS_FOUND_EQUALLY_OFTEN
        .stream()
        .sorted()
        .forEach(word -> expected.add(new KwicResult(word, HOW_OFTEN_THE_EQUAL_WORDS_ARE_FOUND)));
    for (int number = NUMBERED_WORDS; expected.size() < KwicService.MAX_RESULTS; --number) {
      expected.add(new KwicResult(numberedWord(number), number));
    }
    return expected;

  }

  @Test
  @DisplayName("The suggestions for user tasks are the 21 most frequent words, equal ones alphabetical")
  void userTasksSuggestTheMostFrequentWords() {

    final var service = new UserTaskService();
    ReflectionTestUtils.setField(service, "kwicService", new KwicService(mongoTemplate));
    ReflectionTestUtils.setField(service, "listPaths",
        new ListPaths(UserTaskService.FIELDS_OF_THE_LIST, Optional.empty()));

    final var suggestions = service.kwic(UserTaskVisibility.everyUserTask(), null, null, FULL_TEXT, "ord");

    assertThat(suggestions).containsExactlyElementsOf(theTwentyOneMostFrequentWords());

  }

  @Test
  @DisplayName("The suggestions for workflows are the 21 most frequent words, equal ones alphabetical")
  void workflowsSuggestTheMostFrequentWords() {

    final var service = new WorkflowlistService();
    ReflectionTestUtils.setField(service, "kwicService", new KwicService(mongoTemplate));
    ReflectionTestUtils.setField(service, "listPaths",
        new ListPaths(WorkflowlistService.FIELDS_OF_THE_LIST, Optional.empty()));

    final var suggestions = service.kwic(null, WorkflowVisibility.everyWorkflow(), null, FULL_TEXT, "ord");

    assertThat(suggestions).containsExactlyElementsOf(theTwentyOneMostFrequentWords());

  }

}
