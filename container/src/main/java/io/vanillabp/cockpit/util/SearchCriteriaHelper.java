package io.vanillabp.cockpit.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.util.StringUtils;

public class SearchCriteriaHelper {
    public static List<? extends CriteriaDefinition> buildSearchCriteria(
            final Collection<SearchQuery> searchQueries) {

        if ((searchQueries == null)
                || searchQueries.isEmpty()) {
            return List.of();
        }

        final List<CriteriaDefinition> criteriaList = new ArrayList<>();
        final List<String> fullTextTerms = new ArrayList<>();
        TextCriteria textCriteria = TextCriteria.forDefaultLanguage();
        searchQueries.forEach(searchQuery -> {
            if (StringUtils.hasText(searchQuery.path())) {
                criteriaList.add(Criteria
                        .where(searchQuery.path())
                        .regex(searchQuery.query(), searchQuery.caseInsensitive() ? "i" : ""));
            } else if (StringUtils.hasText(searchQuery.query())) {
                String[] parts = searchQuery.query().split("\\s+");
                textCriteria.caseSensitive(!searchQuery.caseInsensitive());
                for (String part : parts) {
                    if (StringUtils.hasText(part) && part.length() > 2 && !fullTextTerms.contains(part)) {
                        fullTextTerms.add(part);
                        textCriteria.matching(part);
                    }
                }

            }
        });
        if (fullTextTerms.size() > 0) {
            criteriaList.add(0, textCriteria);
        }

        return criteriaList;

    }
}
