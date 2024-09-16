package io.vanillabp.cockpit.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.util.StringUtils;

public class SearchCriteriaHelper {
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
