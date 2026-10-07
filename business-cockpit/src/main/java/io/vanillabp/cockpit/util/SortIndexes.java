package io.vanillabp.cockpit.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;

/**
 * The MongoDB indexes which make sorting a list fast. There is one instance per collection: one for
 * user tasks and one for workflows.
 * <p>
 * The field {@code sort} of a list request comes from the client. Every combination of paths the
 * server had not seen before used to get an index of its own, without any check and without any
 * limit. So every logged-in user could create indexes until MongoDB refused any more, at 64 per
 * collection, and every index makes each write a little slower. Two rules stop that:
 * <ul>
 * <li>A path follows the rule of {@link ListPaths}. Anything else is answered with
 * {@code 400 Bad Request}.</li>
 * <li>At most {@code limit} sort indexes exist per collection. Once they are there, a new
 * combination is sorted without an index of its own, and the server warns once per combination.</li>
 * </ul>
 * A sort index is recognised by the prefix {@value #INDEX_PREFIX} of its name. The limit is counted
 * in the database, so several instances of the cockpit share it.
 * <p>
 * See decision 57 in the repository's DECISIONS.md.
 */
public class SortIndexes {

    public static final String INDEX_PREFIX = "_sort_";

    private final String collectionName;

    private final ListPaths listPaths;

    private final int limit;

    private final String limitProperty;

    private final MongoTemplate mongoTemplate;

    private final Logger logger;

    /**
     * The combinations dealt with already: indexed, failed to be indexed, or left without an index
     * because the limit was reached. None of them is looked at again.
     */
    private final Set<String> combinationsDealtWith = new HashSet<>();

    /**
     * @param collectionName The collection the list reads
     * @param listPaths The paths the list may be sorted by
     * @param limit How many sort indexes the collection may have at most
     * @param limitProperty The configuration key of the limit, for the warning
     */
    public SortIndexes(
            final String collectionName,
            final ListPaths listPaths,
            final int limit,
            final String limitProperty,
            final MongoTemplate mongoTemplate,
            final Logger logger) {

        this.collectionName = collectionName;
        this.listPaths = listPaths;
        this.limit = limit;
        this.limitProperty = limitProperty;
        this.mongoTemplate = mongoTemplate;
        this.logger = logger;

    }

    /**
     * Learns the sort indexes which exist already, so they are not created a second time.
     */
    public synchronized void learnExistingIndexes() {

        combinationsDealtWith.addAll(namesOfExistingSortIndexes()
                .stream()
                .map(name -> name.substring(INDEX_PREFIX.length()))
                .toList());

    }

    /**
     * @param path One path of the field {@code sort}
     * @throws io.vanillabp.cockpit.commons.exceptions.BcInvalidRequestException if the list cannot
     *         be sorted by it
     */
    public void checkPath(
            final String path) {

        listPaths.check("sort", path);

    }

    /**
     * Makes sure a combination of sort paths has its index, unless the limit is reached. It is
     * synchronized as a whole, so two requests never count the same free place twice.
     *
     * @param combination The field {@code sort} as the request named it, which names the index
     * @param indexedFields The fields of the index, in their order
     */
    public synchronized void ensureIndex(
            final String combination,
            final List<String> indexedFields) {

        if (!combinationsDealtWith.add(combination)) {
            return;
        }

        final var existing = namesOfExistingSortIndexes().size();
        if (existing >= limit) {
            logger.warn("""
                    The list of collection '{}' is sorted by '{}' without an index of its own. \
                    The collection has {} sort indexes, and '{}' allows {}. Sorting without an index \
                    gets slow on a large collection. Drop sort indexes nobody uses any more (their \
                    names start with '{}'), or raise the limit, for example:

                      {}: {}

                    MongoDB takes 64 indexes per collection at most, and the cockpit needs a few of \
                    them for itself.""",
                    collectionName, combination, existing, limitProperty, limit, INDEX_PREFIX,
                    limitProperty, limit + 10);
            return;
        }

        final var index = new Index();
        indexedFields.forEach(field -> index.on(field, Sort.Direction.ASC));
        index.named(INDEX_PREFIX + combination);
        try {
            mongoTemplate
                    .indexOps(collectionName)
                    .createIndex(index);
        } catch (Exception e) {
            // the list works without the index, and trying again on every request costs time
            logger.error("Could not create the index '{}{}' of collection '{}' for sorting",
                    INDEX_PREFIX, combination, collectionName, e);
        }

    }

    private List<String> namesOfExistingSortIndexes() {

        return mongoTemplate
                .indexOps(collectionName)
                .getIndexInfo()
                .stream()
                .map(IndexInfo::getName)
                .filter(name -> name.startsWith(INDEX_PREFIX))
                .toList();

    }

}
