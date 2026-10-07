package io.vanillabp.cockpit.util;

import io.vanillabp.cockpit.commons.exceptions.BcInvalidRequestException;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/**
 * The paths a client may name in a list request: to sort by, to filter by, or to get suggestions
 * for a search field from. There is one instance per list: one for user tasks and one for
 * workflows.
 * <p>
 * A path names a field of the list, like {@code dueDate} or {@code title.de}, or a key of the
 * business data, like {@code details.customer.name}. Each part of a path holds letters, digits and
 * {@code _} only, and below {@code details.} the {@code -} as well, because a workflow module may
 * report a key like {@code order-id}. Anything else is answered with {@code 400 Bad Request}.
 * <p>
 * Every path goes into a MongoDB query as the name of a field. Without the rule a client could
 * sort by or filter on a field the GUI API never shows, and guess its values from what the list
 * answers. A sort path also creates an index, see {@link SortIndexes}.
 * <p>
 * See decision 57 in the repository's DECISIONS.md.
 */
public class ListPaths {

    /** Where the business data of a user task or a workflow is stored. */
    public static final String BUSINESS_DATA = "details";

    /** Where a filter without a path looks: the full text the workflow module reported. */
    public static final String FULLTEXT = "detailsFulltextSearch";

    /** One part of a path: letters and digits of any language, and the underscore. */
    private static final Pattern PATH = Pattern.compile("[\\p{L}\\p{N}_]+(\\.[\\p{L}\\p{N}_]+)*");

    private final Set<String> fieldsOfTheList;

    private final Optional<String> mapKeyDotReplacement;

    /**
     * @param fieldsOfTheList The top-level fields a path may start with, besides {@value #BUSINESS_DATA}
     * @param mapKeyDotReplacement What the cockpit stores instead of a dot in a key of the business
     *        data, if anything. A path names such a key in its stored form, so the replacement is
     *        allowed in a path below {@value #BUSINESS_DATA}
     */
    public ListPaths(
            final Set<String> fieldsOfTheList,
            final Optional<String> mapKeyDotReplacement) {

        this.fieldsOfTheList = Set.copyOf(fieldsOfTheList);
        this.mapKeyDotReplacement = mapKeyDotReplacement;

    }

    /**
     * @param parameter The part of the request which named the path, for the message
     * @param path The path to check
     * @throws BcInvalidRequestException if the path breaks the rule
     */
    public void check(
            final String parameter,
            final String path) {

        if (isAllowed(path)) {
            return;
        }
        throw new BcInvalidRequestException(
                "'%s' may only name fields of the list or keys below '%s.'. A part of a path holds letters, digits and '_', and below '%s.' also '-'"
                        .formatted(parameter, BUSINESS_DATA, BUSINESS_DATA));

    }

    /**
     * Checks the path of every filter. A filter without a path searches the full text, which is
     * always allowed.
     *
     * @throws BcInvalidRequestException if one path breaks the rule
     */
    public void checkFilters(
            final Collection<SearchQuery> searchQueries) {

        if (searchQueries == null) {
            return;
        }
        searchQueries
                .stream()
                .map(SearchQuery::path)
                .filter(StringUtils::hasText)
                .forEach(path -> check("searchQueries.path", path));

    }

    private boolean isAllowed(
            final String path) {

        if (path == null) {
            return false;
        }
        final var firstDot = path.indexOf('.');
        final var field = firstDot == -1 ? path : path.substring(0, firstDot);
        if (field.equals(BUSINESS_DATA)) {
            // the replacement of a dot and a dash are allowed in a key, so both count as '_' here
            final var storedForm = mapKeyDotReplacement
                    .map(replacement -> path.replace(replacement, "_"))
                    .orElse(path)
                    .replace('-', '_');
            return (firstDot != -1) && PATH.matcher(storedForm).matches();
        }
        return fieldsOfTheList.contains(field) && PATH.matcher(path).matches();

    }

}
