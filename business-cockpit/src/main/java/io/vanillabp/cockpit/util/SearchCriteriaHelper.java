package io.vanillabp.cockpit.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bson.Document;
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
   * without a path searches the full text and the note.
   *
   * @param searchQueries The filters, or {@code null}
   * @return The criteria, an empty list if there are no filters
   */
  public static List<? extends CriteriaDefinition> buildSearchCriteria(
      final Collection<SearchQuery> searchQueries) {

    if ((searchQueries == null) || searchQueries.isEmpty()) {
      return List.of();
    }

    final List<CriteriaDefinition> list = new ArrayList<>(searchQueries
        .stream()
        .map(SearchCriteriaHelper::criterionOf)
        .toList());

    return list;

  }

  /**
   * A filter with a path matches that path. A filter without one searches the full text: the
   * words the workflow module reported, and the note the users keep about the entry.
   */
  private static CriteriaDefinition criterionOf(
      final SearchQuery query) {

    final var options = query.caseInsensitive() ? "i" : "";
    if (StringUtils.hasText(query.path())) {
      return Criteria.where(query.path()).regex(query.query(), options);
    }
    return new FullText(new Criteria().orOperator(
        Criteria.where("detailsFulltextSearch").regex(query.query(), options),
        Criteria.where(NotesOfTheCockpit.FIELD).regex(query.query(), options)));

  }

  /**
   * The "or" of the full-text filter. A query refuses a second criterion without a key, and the
   * visibility of a list is one already. So the "or" carries the key the full-text filter had
   * before it read the note as well. A second filter without a path is refused the same way as
   * before.
   */
  private record FullText(Criteria or) implements CriteriaDefinition {

    @Override
    public Document getCriteriaObject() {
      return or.getCriteriaObject();
    }

    @Override
    public String getKey() {
      return "detailsFulltextSearch";
    }

  }

}
