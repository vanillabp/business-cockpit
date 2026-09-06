package io.vanillabp.cockpit.util.kwic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import io.vanillabp.cockpit.util.SearchCriteriaHelper;
import io.vanillabp.cockpit.util.SearchQuery;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.StringOperators;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.stereotype.Service;

@Service
public class KwicService {

    private final MongoTemplate mongoTemplate;

    public KwicService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<KwicResult> getKwicAggregatedResults(
            final Class<?> searchCollectionClass,
            final CriteriaDefinition matchCriteria,
            final Collection<SearchQuery> searchQueries,
            final String path,
            final String query) {

        final var searchQueryCriterias = SearchCriteriaHelper.buildSearchCriteria(searchQueries);

        final var groupedQuery = "(\\S*" + query + "\\S*)"; // find entire words
        final var aggOperations = new ArrayList<AggregationOperation>();
        // limit results according to regexp and predefined limitations
        aggOperations.add(Aggregation.match(matchCriteria));
        if (searchQueryCriterias != null) {
            searchQueryCriterias.forEach(s -> aggOperations.add(Aggregation.match(s)));
        }
        // add words matching as a new field
        aggOperations.add(Aggregation.addFields().addFieldWithValue("matches", StringOperators.RegexFindAll.valueOf(path).regex(groupedQuery).options("i")).build());
        // drop fields not necessary
        aggOperations.add(Aggregation.project().andExclude("_id").andInclude("matches.captures"));
        // unwind result from find 'all'
        aggOperations.add(Aggregation.unwind("captures"));
        // unwind result from regex group -> may be used in future for other groups if necessary
        aggOperations.add(Aggregation.unwind("captures"));
        // group and count words found
        aggOperations.add(Aggregation.group("captures").count().as("count"));
        aggOperations.add(Aggregation.limit(21));

        return mongoTemplate
                .aggregate(
                        Aggregation.newAggregation(
                                aggOperations
                        ),
                        searchCollectionClass,
                        KwicResult.class)
                .getMappedResults()
                .stream()
                .sorted(Comparator
                        .comparingInt(KwicResult::count)
                        .thenComparing(KwicResult::item))
                .toList();
    }
}
