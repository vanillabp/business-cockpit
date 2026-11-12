package io.vanillabp.cockpit.util.kwic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.StringOperators;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.stereotype.Service;

import io.vanillabp.cockpit.util.SearchCriteriaHelper;
import io.vanillabp.cockpit.util.SearchQuery;

/**
 * Finds suggestions for a search field of a list (KWIC, "keyword in context"). It looks for the
 * whole words which contain what the user typed so far and counts how often each word is found.
 */
@Service
public class KwicService {

  /**
   * How many words a search returns at most. A user interface shows twenty and takes a
   * twenty-first word as the sign that there are more.
   */
  public static final int MAX_RESULTS = 21;

  private final MongoTemplate mongoTemplate;

  /**
   * Built by Spring.
   *
   * @param mongoTemplate Used to run the search in MongoDB
   */
  public KwicService(
      MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  /**
   * Finds the words in a field which contain the text typed so far. The search ignores case. It
   * returns the {@value #MAX_RESULTS} words found most often, the most frequent first.
   *
   * @param searchCollectionClass The document class whose collection is searched
   * @param matchCriteria Limits the search to the documents the user may see
   * @param searchQueries The filters the user set in the list, or {@code null}
   * @param path The field to look for words in
   * @param query The text typed so far, taken literally
   * @return The words found, the most frequent first. Words found equally often are in
   *         alphabetical order, upper case before lower case.
   */
  public List<KwicResult> getKwicAggregatedResults(
      final Class<?> searchCollectionClass,
      final CriteriaDefinition matchCriteria,
      final Collection<SearchQuery> searchQueries,
      final String path,
      final String query) {

    final var searchQueryCriterias = SearchCriteriaHelper.buildSearchCriteria(searchQueries);

    final var groupedQuery = "(\\S*"
        + SearchCriteriaHelper.literally(query)
        + "\\S*)"; // find entire words
    final var aggOperations = new ArrayList<AggregationOperation>();
    // limit results according to regexp and predefined limitations
    aggOperations.add(Aggregation.match(matchCriteria));
    if (searchQueryCriterias != null) {
      searchQueryCriterias.forEach(s -> aggOperations.add(Aggregation.match(s)));
    }
    // add words matching as a new field
    aggOperations.add(Aggregation.addFields()
        .addFieldWithValue("matches", StringOperators.RegexFindAll.valueOf(path).regex(groupedQuery).options("i"))
        .build());
    // drop fields not necessary
    aggOperations.add(Aggregation.project().andExclude("_id").andInclude("matches.captures"));
    // unwind result from find 'all'
    aggOperations.add(Aggregation.unwind("captures"));
    // unwind result from regex group -> may be used in future for other groups if necessary
    aggOperations.add(Aggregation.unwind("captures"));
    // group and count words found
    aggOperations.add(Aggregation.group("captures").count().as("count"));
    // MongoDB gives no order after a group, so a limit without a sort keeps any words.
    // A limit right after a sort keeps only the best few words in memory.
    aggOperations.add(Aggregation.sort(Sort.by(
        Order.desc("count"),
        Order.asc("_id"))));
    aggOperations.add(Aggregation.limit(MAX_RESULTS));

    return mongoTemplate
        .aggregate(
            Aggregation.newAggregation(
                aggOperations
            ),
            searchCollectionClass,
            KwicResult.class)
        .getMappedResults();
  }
}
