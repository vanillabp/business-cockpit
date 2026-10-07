package io.vanillabp.cockpit.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.vanillabp.cockpit.commons.exceptions.BcInvalidRequestException;
import io.vanillabp.integration.test.utils.ContainerImages;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;

/**
 * Which paths a list may be sorted by, and how many indexes sorting may create. The paths come
 * from the request of a client, so every logged-in user could create indexes until MongoDB refused
 * any more. The tests run against a real MongoDB, because the limit is counted there.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class SortIndexesTest {

    private static final String COLLECTION = "usertask";

    private static final String LIMIT_PROPERTY = "business-cockpit.mongodb.sort-indexes-per-collection";

    private static final Set<String> FIELDS = Set.of("dueDate", "title", "assignee");

    private static final org.testcontainers.mongodb.MongoDBContainer MONGODB =
            new org.testcontainers.mongodb.MongoDBContainer(ContainerImages.MONGODB);

    private static MongoClient mongoClient;

    private MongoTemplate mongoTemplate;

    private Logger logger;

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

        mongoTemplate = new MongoTemplate(mongoClient, "cockpit-" + UUID.randomUUID());
        mongoTemplate.createCollection(COLLECTION);
        logger = mock(Logger.class);

    }

    private SortIndexes sortIndexes(
            final int limit,
            final Optional<String> mapKeyDotReplacement) {

        final var sortIndexes = new SortIndexes(
                COLLECTION, new ListPaths(FIELDS, mapKeyDotReplacement), limit, LIMIT_PROPERTY, mongoTemplate, logger);
        sortIndexes.learnExistingIndexes();
        return sortIndexes;

    }

    private List<String> sortIndexNames() {

        return mongoTemplate
                .indexOps(COLLECTION)
                .getIndexInfo()
                .stream()
                .map(IndexInfo::getName)
                .filter(name -> name.startsWith(SortIndexes.INDEX_PREFIX))
                .toList();

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "dueDate", "title.de", "assignee.sort", "details.customer", "details.customer.name",
            "details.order_id", "details.order-id", "details.straße", "details.position2" })
    void aFieldOfTheListOrAKeyOfTheBusinessDataIsAllowed(
            final String path) {

        assertThatCode(() -> sortIndexes(30, Optional.empty()).checkPath(path))
                .doesNotThrowAnyException();

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "noSuchField", "details", "details.", "details..name", "details.a b", "details.$where",
            "title.en-GB", "assignee-id", "title.$de", " dueDate", "dueDate ", "details.order~id", "_id", "$natural" })
    void anythingElseIsRefusedWithAMessageWhichSaysWhatIsAllowed(
            final String path) {

        assertThatThrownBy(() -> sortIndexes(30, Optional.empty()).checkPath(path))
                .isInstanceOf(BcInvalidRequestException.class)
                .hasMessage("'sort' may only name fields of the list or keys below 'details.'. A part of a "
                        + "path holds letters, digits and '_', and below 'details.' also '-'");

    }

    /**
     * With a replacement for the dot configured, a key like {@code order.id} is stored as
     * {@code order~id}, and a column has to name it like that.
     */
    @Test
    void theReplacementOfADotIsAllowedBelowTheBusinessDataOnly() {

        final var sortIndexes = sortIndexes(30, Optional.of("~"));

        assertThatCode(() -> sortIndexes.checkPath("details.order~id")).doesNotThrowAnyException();
        assertThatThrownBy(() -> sortIndexes.checkPath("title~de"))
                .isInstanceOf(BcInvalidRequestException.class);

    }

    @Test
    void aNewCombinationGetsAnIndexOnce() {

        final var sortIndexes = sortIndexes(30, Optional.empty());

        sortIndexes.ensureIndex("details.customer", List.of("details.customer", "dueDate", "_id"));
        sortIndexes.ensureIndex("details.customer", List.of("details.customer", "dueDate", "_id"));

        assertThat(sortIndexNames()).containsExactly("_sort_details.customer");

    }

    /** A previous start created the index, and this start learns about it instead of trying again. */
    @Test
    void anIndexOfAPreviousStartIsNotCreatedAgain() {

        sortIndexes(30, Optional.empty()).ensureIndex("title", List.of("title", "_id"));

        // MongoDB refuses a second index of the same name over other fields, which would be logged
        sortIndexes(30, Optional.empty()).ensureIndex("title", List.of("title", "dueDate", "_id"));

        assertThat(sortIndexNames()).containsExactly("_sort_title");
        verify(logger, never()).error(anyString(), any(Object[].class));

    }

    @Test
    void theLimitStopsNewIndexesAndWarnsOncePerCombination() {

        final var sortIndexes = sortIndexes(2, Optional.empty());

        sortIndexes.ensureIndex("details.a", List.of("details.a", "_id"));
        sortIndexes.ensureIndex("details.b", List.of("details.b", "_id"));
        sortIndexes.ensureIndex("details.c", List.of("details.c", "_id"));
        sortIndexes.ensureIndex("details.c", List.of("details.c", "_id"));
        sortIndexes.ensureIndex("details.d", List.of("details.d", "_id"));

        assertThat(sortIndexNames()).containsExactlyInAnyOrder("_sort_details.a", "_sort_details.b");
        verify(logger, times(2)).warn(anyString(), any(Object[].class));

    }

    @Test
    void theLimitIsCountedInTheDatabaseSoInstancesShareIt() {

        final var oneInstance = sortIndexes(2, Optional.empty());
        final var anotherInstance = sortIndexes(2, Optional.empty());

        oneInstance.ensureIndex("details.a", List.of("details.a", "_id"));
        anotherInstance.ensureIndex("details.b", List.of("details.b", "_id"));
        oneInstance.ensureIndex("details.c", List.of("details.c", "_id"));

        assertThat(sortIndexNames()).containsExactlyInAnyOrder("_sort_details.a", "_sort_details.b");

    }

    @Test
    void aLimitOfZeroCreatesNoIndexAtAll() {

        final var sortIndexes = sortIndexes(0, Optional.empty());

        sortIndexes.ensureIndex("dueDate", List.of("dueDate", "_id"));

        assertThat(sortIndexNames()).isEmpty();
        verify(logger, times(1)).warn(anyString(), any(Object[].class));
        verify(logger, never()).error(anyString(), any(Object[].class));

    }

}
