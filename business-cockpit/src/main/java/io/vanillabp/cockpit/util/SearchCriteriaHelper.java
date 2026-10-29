package io.vanillabp.cockpit.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.util.StringUtils;

/**
 * Turns the filters of a list request into MongoDB criteria.
 */
public class SearchCriteriaHelper {

    /**
     * Only static methods, so nobody needs an instance.
     */
    private SearchCriteriaHelper() {

    }

    /**
     * Creates one criterion per filter. The query of a filter is a regular expression. A filter
     * without a path searches the full text.
     *
     * @param searchQueries The filters, or {@code null}
     * @return The criteria, an empty list if there are no filters
     */
    public static List<? extends CriteriaDefinition> buildSearchCriteria(
            final Collection<SearchQuery> searchQueries) {

        if ((searchQueries == null)
                || searchQueries.isEmpty()) {
            return List.of();
        }

        final List<CriteriaDefinition> list = new ArrayList<>(searchQueries
                .stream()
                .map(query -> Criteria
                        .where(StringUtils.hasText(query.path()) ? query.path() : "detailsFulltextSearch")
                        .regex(query.query(), query.caseInsensitive() ? "i" : ""))
                .toList());

        return list;

    }
}
