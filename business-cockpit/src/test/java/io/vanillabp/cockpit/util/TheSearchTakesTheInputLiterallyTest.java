package io.vanillabp.cockpit.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.testcontainers.mongodb.MongoDBContainer;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import io.vanillabp.cockpit.mongo.converters.BigDecimalReadConverter;
import io.vanillabp.cockpit.mongo.converters.BigDecimalWriteConverter;
import io.vanillabp.cockpit.mongo.converters.OffsetDateTimeReadConverter;
import io.vanillabp.cockpit.mongo.converters.OffsetDateTimeWriteConverter;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.util.kwic.KwicResult;
import io.vanillabp.cockpit.util.kwic.KwicService;
import io.vanillabp.integration.test.utils.ContainerImages;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A search finds the text the user typed, character by character. Characters which mean something
 * in a regular expression mean nothing in a search. Measured against a real MongoDB, because
 * MongoDB runs the expression with its own engine and not with Java's.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class TheSearchTakesTheInputLiterallyTest {

  private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-10-04T08:00:00Z");

  /** A text with every character the tests type which means something in a regular expression. */
  private static final String SPECIAL_CHARACTERS = "Order 4.7 (draft) + 2 * x C:\\Temp\\new";

  private static final MongoDBContainer MONGODB = new MongoDBContainer(ContainerImages.MONGODB);

  private static MongoClient mongoClient;

  private static MongoTemplate mongoTemplate;

  @BeforeAll
  static void startMongoDbWithTasks() {

    MONGODB.start();
    mongoClient = MongoClients.create(MONGODB.getConnectionString());
    mongoTemplate = cockpitLikeTemplate();
    mongoTemplate.save(aTask("meier", "Meier"));
    mongoTemplate.save(aTask("special", SPECIAL_CHARACTERS));
    mongoTemplate.save(aTask("pattern", "pattern (a+)+$ as text"));
    // what the pattern (a+)+$ takes very long to give up on
    mongoTemplate.save(aTask("many-a", "a".repeat(40)
        + "!"));

  }

  @AfterAll
  static void stopMongoDb() {

    if (mongoClient != null) {
      mongoClient.close();
    }
    MONGODB.stop();

  }

  /** A template with the converters of {@code MongoDbConfiguration}, in a database of its own. */
  private static MongoTemplate cockpitLikeTemplate() {

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
    return new MongoTemplate(factory, converter);

  }

  /** A task whose business data and full text both hold the given text. */
  private static UserTask aTask(
      final String userTaskId,
      final String text) {

    final var task = new UserTask();
    task.setId(userTaskId);
    task.setCreatedAt(CREATED_AT);
    task.setWorkflowModuleId("taxi-ride");
    task.setBpmnProcessId("ride");
    task.setTitle(Map.of("en", "Assign a driver"));
    task.setDetails(Map.of("name", text));
    task.setDetailsFulltextSearch(text);
    return task;

  }

  /** What the list of user tasks finds for one filter on a field, or on the full text without a path. */
  private static List<String> idsFoundBy(
      final String path,
      final String text,
      final boolean caseInsensitive) {

    final var query = new Query();
    SearchCriteriaHelper
        .buildSearchCriteria(List.of(new SearchQuery(path, text, caseInsensitive)))
        .forEach(query::addCriteria);
    return mongoTemplate.find(query, UserTask.class).stream().map(UserTask::getId).sorted().toList();

  }

  private static List<String> wordsSuggestedFor(
      final String text) {

    return new KwicService(mongoTemplate)
        .getKwicAggregatedResults(UserTask.class, new Criteria(), List.of(), "detailsFulltextSearch", text)
        .stream()
        .map(KwicResult::item)
        .sorted()
        .toList();

  }

  @Test
  @DisplayName("A dot is a dot: 'Mei.r' does not find 'Meier'")
  void aDotDoesNotStandForAnyCharacter() {

    assertThat(idsFoundBy("details.name", "Mei.r", true)).isEmpty();
    assertThat(idsFoundBy(null, "Mei.r", true)).isEmpty();
    assertThat(wordsSuggestedFor("Mei.r")).isEmpty();

  }

  @Test
  @DisplayName("Dots, brackets, plus, star and backslash find exactly the text typed")
  void specialCharactersFindThemselves() {

    for (final var typed : List.of("4.7", "(draft)", "+ 2 *", "C:\\Temp\\new", SPECIAL_CHARACTERS)) {
      assertThat(idsFoundBy("details.name", typed, false)).as("field, '%s'", typed).containsExactly("special");
      assertThat(idsFoundBy(null, typed, false)).as("full text, '%s'", typed).containsExactly("special");
    }
    assertThat(wordsSuggestedFor("(dra")).containsExactly("(draft)");
    assertThat(wordsSuggestedFor("\\Te")).containsExactly("C:\\Temp\\new");

  }

  @Test
  @DisplayName("A pattern like '(a+)+$' is searched for as text")
  void aPatternIsSearchedForAsText() {

    assertThat(idsFoundBy("details.name", "(a+)+$", false)).containsExactly("pattern");
    assertThat(idsFoundBy(null, "(a+)+$", false)).containsExactly("pattern");
    assertThat(wordsSuggestedFor("(a+)+$")).containsExactly("(a+)+$");

  }

  @Test
  @DisplayName("Whether the search ignores case stays the caller's choice")
  void caseIsIgnoredOnlyWhenAskedFor() {

    assertThat(idsFoundBy("details.name", "MEIER", true)).containsExactly("meier");
    assertThat(idsFoundBy("details.name", "MEIER", false)).isEmpty();
    assertThat(wordsSuggestedFor("MEI")).containsExactly("Meier");

  }

  @Test
  @DisplayName("Letters, digits, spaces and letters outside ASCII get no backslash")
  void onlyAsciiPunctuationGetsABackslash() {

    assertThat(SearchCriteriaHelper.literally("Müller 42")).isEqualTo("Müller 42");
    assertThat(SearchCriteriaHelper.literally("a.b(c)+*?[]{}|^$\\"))
        .isEqualTo("a\\.b\\(c\\)\\+\\*\\?\\[\\]\\{\\}\\|\\^\\$\\\\");
    assertThat(SearchCriteriaHelper.literally(null)).isEmpty();

  }

}
