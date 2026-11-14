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
   * Creates one criterion per filter. The query of a filter is plain text: a field matches if it
   * contains the text. A filter without a path searches the full text and the note.
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
   * The condition of a list which shows the entries of some workflow modules only. It is a filter
   * like the others: it narrows what the visibility of the list lets through and never widens it.
   *
   * @param workflowModuleIds The ids of the workflow modules, or {@code null}
   * @return One criterion, or an empty list for {@code null} or no ids. No ids means every workflow
   *         module, so a request which does not send the limit gets the list it always got.
   */
  public static List<Criteria> onlyTheWorkflowModules(
      final Collection<String> workflowModuleIds) {

    if ((workflowModuleIds == null) || workflowModuleIds.isEmpty()) {
      return List.of();
    }
    return List.of(Criteria.where("workflowModuleId").in(workflowModuleIds));

  }

  /**
   * A filter with a path matches that path. A filter without one searches the full text: the
   * words the workflow module reported, and the note the users keep about the entry.
   */
  private static CriteriaDefinition criterionOf(
      final SearchQuery query) {

    final var options = query.caseInsensitive() ? "i" : "";
    if (StringUtils.hasText(query.path())) {
      return Criteria.where(query.path()).regex(literally(query.query()), options);
    }
    return new FullText(new Criteria().orOperator(
        Criteria.where("detailsFulltextSearch").regex(literally(query.query()), options),
        Criteria.where(NotesOfTheCockpit.FIELD).regex(literally(query.query()), options)));

  }

  /**
   * Turns what a user typed into a regular expression which matches exactly that text. Every
   * place which puts a user's input into a regular expression for MongoDB uses this method.
   * <p>
   * Without it, a character like {@code .}, {@code (} or {@code +} would not find itself, and a
   * user could send a pattern like {@code (a+)+$} which keeps the database busy for a long time.
   * <p>
   * MongoDB runs the expression with its own engine, PCRE, not with Java's. So this method does
   * not use {@link java.util.regex.Pattern#quote(String)}. It puts a backslash in front of every
   * ASCII punctuation character. Both engines read a backslash in front of such a character as
   * "this character itself". Letters, digits, spaces and everything outside ASCII stay as they
   * are.
   *
   * @param text What the user typed, or {@code null}
   * @return A regular expression which matches the text, an empty string for {@code null}
   */
  public static String literally(
      final String text) {

    if (text == null) {
      return "";
    }
    final var result = new StringBuilder(text.length() * 2);
    text.chars().forEach(character -> {
      if (needsABackslash(character)) {
        result.append('\\');
      }
      result.append((char) character);
    });
    return result.toString();

  }

  /**
   * ASCII punctuation, which holds every character with a meaning in a regular expression.
   */
  private static boolean needsABackslash(
      final int character) {

    if ((character >= 128) || Character.isLetterOrDigit(character)) {
      return false;
    }
    return !Character.isWhitespace(character) && !Character.isISOControl(character);

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
