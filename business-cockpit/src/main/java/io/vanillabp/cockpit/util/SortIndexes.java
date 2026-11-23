package io.vanillabp.cockpit.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.util.StringUtils;

import io.vanillabp.cockpit.exceptions.BcInvalidRequestException;

/**
 * The MongoDB indexes which make sorting a list fast. There is one instance per collection: one for
 * user tasks and one for workflows.
 * <p>
 * The field {@code sort} of a list request comes from the client. Every combination of paths the
 * server had not seen before used to get an index of its own, without any check and without any
 * limit. So every logged-in user could create indexes until MongoDB refused any more, at 64 per
 * collection, and every index makes each write a little slower. Three rules stop that:
 * <ul>
 * <li>The field {@code sort} holds at most {@value #LONGEST_SORT} characters and at most
 * {@value #MOST_PATHS} paths. A longer one is answered with {@code 400 Bad Request}.</li>
 * <li>A path follows the rule of {@link ListPaths}. Anything else is answered with
 * {@code 400 Bad Request}.</li>
 * <li>At most {@code limit} sort indexes exist per collection. Once they are there, a new
 * combination is sorted without an index of its own, and the server warns once per combination.</li>
 * </ul>
 * A sort index is recognised by the prefix {@value #INDEX_PREFIX} of its name. The limit is counted
 * in the database, so several instances of the cockpit share it.
 * <p>
 * Every combination comes from a request, so whatever the class remembers about them has a limit
 * as well. The combinations with an index are as many as the indexes. Of those left without an
 * index, only the last {@value #MOST_COMBINATIONS_REMEMBERED_WITHOUT_AN_INDEX} are remembered.
 * <p>
 * See decision 57 in the repository's DECISIONS.md.
 */
public class SortIndexes {

  /**
   * The prefix of the name of every sort index. The rest of the name is the sort combination.
   * <p>
   * Released database changesets read this value. Changing it changes what they do on a new
   * database, without a line of them changing, so it stays as it is. {@code NamesTheReleasedChangesetsReadTest}
   * fails when it changes.
   */
  public static final String INDEX_PREFIX = "_sort_";

  /** The most characters the field {@code sort} of a list request holds. */
  public static final int LONGEST_SORT = 500;

  /** The most paths the field {@code sort} of a list request names. */
  public static final int MOST_PATHS = 5;

  /**
   * How many combinations without an index of their own are remembered, so the warning about each
   * of them is written once. The oldest one is forgotten first, and warned about again if it
   * comes back.
   */
  public static final int MOST_COMBINATIONS_REMEMBERED_WITHOUT_AN_INDEX = 1_000;

  private final String collectionName;

  private final ListPaths listPaths;

  private final int limit;

  private final String limitProperty;

  private final MongoTemplate mongoTemplate;

  private final Logger logger;

  /**
   * The combinations which have their index, found in the database or created by this instance.
   * They are no more than the indexes MongoDB takes per collection.
   */
  private final Set<String> combinationsIndexed = new HashSet<>();

  /**
   * The last combinations which were left without an index, because the limit was reached or the
   * index could not be created. None of them is looked at again while it is remembered.
   */
  private final Set<String> combinationsWithoutAnIndex = Collections.newSetFromMap(
      new LinkedHashMap<String, Boolean>() {

        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(
            final Map.Entry<String, Boolean> eldest) {
          return size() > MOST_COMBINATIONS_REMEMBERED_WITHOUT_AN_INDEX;
        }

      });

  /**
   * Creates the sort indexes of one collection.
   *
   * @param collectionName The collection the list reads
   * @param listPaths The paths the list may be sorted by
   * @param limit How many sort indexes the collection may have at most
   * @param limitProperty The configuration key of the limit, for the warning
   * @param mongoTemplate Used to read and create the indexes
   * @param logger Where the warnings go
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

    combinationsIndexed.addAll(namesOfExistingSortIndexes()
        .stream()
        .map(name -> name.substring(INDEX_PREFIX.length()))
        .toList());

  }

  /**
   * Checks the size of the field {@code sort} as a whole, before its paths are looked at. The
   * message does not repeat the field, because it can be long.
   *
   * @param sort The field {@code sort} of a list request, or {@code null}
   * @throws BcInvalidRequestException if the field holds too many characters or paths
   */
  public void checkSort(
      final String sort) {

    if (sort == null) {
      return;
    }
    if (sort.length() > LONGEST_SORT) {
      throw new BcInvalidRequestException(
          "'sort' holds at most %d characters".formatted(LONGEST_SORT));
    }
    if (Arrays.stream(sort.split(",")).filter(StringUtils::hasText).count() > MOST_PATHS) {
      throw new BcInvalidRequestException(
          "'sort' names at most %d paths".formatted(MOST_PATHS));
    }

  }

  /**
   * Checks one path a client wants to sort by.
   *
   * @param path One path of the field {@code sort}
   * @throws io.vanillabp.cockpit.exceptions.BcInvalidRequestException if the list cannot
   *         be sorted by it
   */
  public void checkPath(
      final String path) {

    listPaths.check("sort", path);

  }

  /**
   * Makes sure a combination of sort paths has its index, unless the limit is reached. It is
   * synchronized as a whole, so two requests never count the same free place twice. The caller
   * has checked the combination with {@link #checkSort(String)} and {@link #checkPath(String)}
   * before, so the combination is short and holds allowed characters only when it is logged.
   *
   * @param combination The field {@code sort} as the request named it, which names the index
   * @param indexedFields The fields of the index, in their order
   */
  public synchronized void ensureIndex(
      final String combination,
      final List<String> indexedFields) {

    if (combinationsIndexed.contains(combination) || combinationsWithoutAnIndex.contains(combination)) {
      return;
    }

    // remembered only once dealt with: a failure to read the indexes leaves it for the next request
    final var existing = namesOfExistingSortIndexes().size();
    if (existing >= limit) {
      combinationsWithoutAnIndex.add(combination);
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
      combinationsIndexed.add(combination);
    } catch (Exception e) {
      // the list works without the index, and trying again on every request costs time
      combinationsWithoutAnIndex.add(combination);
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
