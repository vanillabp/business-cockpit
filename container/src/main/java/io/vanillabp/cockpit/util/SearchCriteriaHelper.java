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

    public static List<Criteria> getWorkflowModuleIdCriteria(
            final List<String> workflowModuleIds) {

        if ((workflowModuleIds == null)
                || workflowModuleIds.isEmpty()) {
            return null;
        }

        final List<Criteria> result = new ArrayList<>();
        addWorkflowModuleIdCriteria(result, workflowModuleIds);
        return result;

    }

    public static void addWorkflowModuleIdCriteria(
            final List<Criteria> subCriterias,
            final List<String> workflowModuleIds) {

        if ((workflowModuleIds == null)
                || workflowModuleIds.isEmpty()) {
            return;
        }

        subCriterias
                .add(Criteria
                        .where("workflowModuleId")
                        .in(workflowModuleIds));

    }

}
