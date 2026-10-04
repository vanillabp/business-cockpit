package io.vanillabp.cockpit.tasklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.vanillabp.cockpit.commons.mongo.converters.BigDecimalReadConverter;
import io.vanillabp.cockpit.commons.mongo.converters.BigDecimalWriteConverter;
import io.vanillabp.cockpit.commons.mongo.converters.OffsetDateTimeReadConverter;
import io.vanillabp.cockpit.commons.mongo.converters.OffsetDateTimeWriteConverter;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.util.SearchCriteriaHelper;
import io.vanillabp.cockpit.util.SearchQuery;
import io.vanillabp.cockpit.util.kwic.KwicResult;
import io.vanillabp.cockpit.util.kwic.KwicService;
import io.vanillabp.integration.test.utils.ContainerImages;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * What happens to a key with a dot in the business data of a user task, today and with the two
 * settings Spring Data offers for it. Measured for story 1442 against a real MongoDB. Nothing of
 * the cockpit was changed for it, so every test describes the behaviour of today's code and stays
 * green as long as that behaviour stays the same.
 * <p>
 * The business data, {@code details}, is a map the workflow module fills. Today the cockpit sets
 * neither of the two settings, so Spring Data refuses a key like {@code order.id} before anything
 * is sent. {@code WhatMongoDbRefusesEveryTimeTest} shows what the service makes of that.
 * <p>
 * The two settings are these:
 * <ul>
 * <li>{@code preserveMapKeys(true)} writes the key as it is. MongoDB has taken dots in field names
 * since version 5. The key comes back unchanged, but no query of the cockpit can reach it, because
 * the cockpit addresses business data by a path like {@code details.order.id}, and MongoDB reads
 * every dot of a path as a step into a nested document.</li>
 * <li>{@code setMapKeyDotReplacement(...)} writes the key with the dot replaced. The key comes back
 * with the dot, but a key which held the replacement already comes back with a dot too. The
 * stored form is what a query sees, so a path with the dot finds nothing either.</li>
 * </ul>
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class AKeyWithADotInTheDetailsTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-10-04T08:00:00Z");

    /** What the replacement tests replace a dot with. Any string shows the same. */
    private static final String REPLACEMENT = "~";

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
     * A template with the converters of {@code MongoDbConfiguration}, in a database of its own.
     *
     * @param setting What is set on the converter on top of what the cockpit sets
     */
    private static MongoTemplate cockpitLikeTemplate(
            final Consumer<MappingMongoConverter> setting) {

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
        setting.accept(converter);
        converter.afterPropertiesSet();
        return new MongoTemplate(factory, converter);

    }

    private static MongoTemplate keysPreserved() {

        return cockpitLikeTemplate(converter -> converter.preserveMapKeys(true));

    }

    private static MongoTemplate dotsReplaced() {

        return cockpitLikeTemplate(converter -> converter.setMapKeyDotReplacement(REPLACEMENT));

    }

    private static UserTask aTask(
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

    /** The details as MongoDB holds them, read without Spring Data's mapping. */
    private static Document storedDetails(
            final MongoTemplate mongoTemplate,
            final String userTaskId) {

        final var stored = mongoTemplate
                .getCollection(UserTask.COLLECTION_NAME)
                .find(new Document("_id", userTaskId))
                .first();
        assertThat(stored).as("the task '%s' is stored", userTaskId).isNotNull();
        return stored.get("details", Document.class);

    }

    /** What the list of user tasks finds for one search of the graphical user interface. */
    private static List<String> idsFoundBy(
            final MongoTemplate mongoTemplate,
            final String path,
            final String text) {

        final var query = new Query();
        SearchCriteriaHelper
                .buildSearchCriteria(List.of(new SearchQuery(path, text, false)))
                .forEach(query::addCriteria);
        return mongoTemplate.find(query, UserTask.class).stream().map(UserTask::getId).toList();

    }

    private static List<String> idsSortedBy(
            final MongoTemplate mongoTemplate,
            final String path) {

        final var query = new Query().with(Sort.by(Sort.Order.asc(path), Sort.Order.asc("_id")));
        return mongoTemplate.find(query, UserTask.class).stream().map(UserTask::getId).toList();

    }

    private static List<String> wordsSuggestedFor(
            final MongoTemplate mongoTemplate,
            final String path,
            final String text) {

        return new KwicService(mongoTemplate)
                .getKwicAggregatedResults(UserTask.class, new Criteria(), List.of(), path, text)
                .stream()
                .map(KwicResult::item)
                .toList();

    }

    @Test
    @DisplayName("Today Spring Data refuses the key before anything reaches MongoDB")
    void todayTheKeyIsRefused() {

        final var mongoTemplate = cockpitLikeTemplate(converter -> {});

        assertThatThrownBy(() -> mongoTemplate.save(aTask("task-1", Map.of("order.id", "4711"))))
                .isInstanceOf(MappingException.class)
                .hasMessageContaining("Map key order.id contains dots but no replacement was configured");
        assertThat(mongoTemplate.findById("task-1", UserTask.class)).isNull();

    }

    @Test
    @DisplayName("With the keys preserved, MongoDB stores the key with its dot and it comes back unchanged")
    void aPreservedKeyIsStoredAndReadAsItIs() {

        final var mongoTemplate = keysPreserved();
        final var details = Map.<String, Object>of(
                "order.id", "4711",
                "customer", Map.of("address.city", "Vienna"));

        mongoTemplate.save(aTask("task-1", details));

        final var stored = storedDetails(mongoTemplate, "task-1");
        assertThat(stored.keySet()).containsExactlyInAnyOrder("order.id", "customer");
        assertThat(stored.get("customer", Document.class).keySet())
                .as("a key in a nested map is preserved as well")
                .containsExactly("address.city");
        assertThat(mongoTemplate.findById("task-1", UserTask.class).getDetails()).isEqualTo(details);

    }

    @Test
    @DisplayName("With the keys preserved, a search by the path of the key finds nothing, or a task it was not meant for")
    void aSearchCannotReachAPreservedKey() {

        final var mongoTemplate = keysPreserved();
        mongoTemplate.save(aTask("dotted-key", Map.of("order.id", "4711")));
        mongoTemplate.save(aTask("nested-map", Map.of("order", Map.of("id", "4711"))));

        assertThat(idsFoundBy(mongoTemplate, "details.order.id", "4711"))
                .as("MongoDB reads the path as 'order', then 'id', which is the nested map")
                .containsExactly("nested-map");
        assertThat(wordsSuggestedFor(mongoTemplate, "details.order.id", "47"))
                .as("the words suggested while typing come from the nested map alone")
                .containsExactly("4711");

    }

    @Test
    @DisplayName("With the keys preserved, a sort by the path of the key leaves the tasks in the order of their ids")
    void aSortCannotReachAPreservedKey() {

        final var mongoTemplate = keysPreserved();
        mongoTemplate.save(aTask("task-1", Map.of("order.id", "b")));
        mongoTemplate.save(aTask("task-2", Map.of("order.id", "a")));

        assertThat(idsSortedBy(mongoTemplate, "details.order.id"))
                .as("a sort by the value would put task-2 first")
                .containsExactly("task-1", "task-2");

    }

    @Test
    @DisplayName("With the keys preserved, MongoDB reaches the key only through $getField, which the cockpit does not build")
    void onlyGetFieldReachesAPreservedKey() {

        final var mongoTemplate = keysPreserved();
        mongoTemplate.save(aTask("dotted-key", Map.of("order.id", "4711")));
        mongoTemplate.save(aTask("nested-map", Map.of("order", Map.of("id", "4711"))));

        final var query = new BasicQuery(
                "{ $expr: { $eq: [ { $getField: { field: 'order.id', input: '$details' } }, '4711' ] } }");

        assertThat(mongoTemplate.find(query, UserTask.class).stream().map(UserTask::getId))
                .containsExactly("dotted-key");

    }

    @Test
    @DisplayName("The full-text search does not care about keys, because the workflow module fills its text")
    void theFullTextSearchIsNotAboutKeys() {

        final var mongoTemplate = keysPreserved();
        final var task = aTask("task-1", Map.of("order.id", "4711"));
        task.setDetailsFulltextSearch("order 4711");
        mongoTemplate.save(task);

        assertThat(idsFoundBy(mongoTemplate, null, "4711")).containsExactly("task-1");
        assertThat(wordsSuggestedFor(mongoTemplate, "detailsFulltextSearch", "47")).containsExactly("4711");

    }

    @Test
    @DisplayName("With a replacement, MongoDB holds the replaced key and Spring Data gives back the dot")
    void aReplacedKeyIsStoredReplacedAndReadWithTheDot() {

        final var mongoTemplate = dotsReplaced();
        final var details = Map.<String, Object>of("order.id", "4711");

        mongoTemplate.save(aTask("task-1", details));

        assertThat(storedDetails(mongoTemplate, "task-1").keySet()).containsExactly("order" + REPLACEMENT + "id");
        assertThat(mongoTemplate.findById("task-1", UserTask.class).getDetails())
                .as("the repository, the graphical user interface and the change stream read through the converter")
                .isEqualTo(details);

    }

    @Test
    @DisplayName("With a replacement, a key which held the replacement already comes back with a dot")
    void aKeyWithTheReplacementComesBackChanged() {

        final var mongoTemplate = dotsReplaced();

        mongoTemplate.save(aTask("task-1", Map.of("order" + REPLACEMENT + "id", "4711")));

        assertThat(storedDetails(mongoTemplate, "task-1").keySet()).containsExactly("order" + REPLACEMENT + "id");
        assertThat(mongoTemplate.findById("task-1", UserTask.class).getDetails().keySet())
                .as("reading cannot tell a replaced dot from the same characters sent by the module")
                .containsExactly("order.id");

    }

    @Test
    @DisplayName("With a replacement, a search finds the key only by the stored form of its path")
    void aSearchReachesAReplacedKeyOnlyByItsStoredForm() {

        final var mongoTemplate = dotsReplaced();
        mongoTemplate.save(aTask("task-1", Map.of("order.id", "4711")));

        assertThat(idsFoundBy(mongoTemplate, "details.order.id", "4711"))
                .as("the path the module names, with the dot")
                .isEmpty();
        assertThat(idsFoundBy(mongoTemplate, "details.order" + REPLACEMENT + "id", "4711"))
                .as("the path as it is stored")
                .containsExactly("task-1");
        assertThat(idsSortedBy(mongoTemplate, "details.order" + REPLACEMENT + "id")).containsExactly("task-1");

    }

}
