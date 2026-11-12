package io.vanillabp.cockpit.util;

/**
 * One filter of a list request.
 *
 * @param path The field to filter on. Without a path, the full text is searched.
 * @param query The text the field has to contain, taken literally
 * @param caseInsensitive Whether the match ignores case
 */
public record SearchQuery(String path, String query, boolean caseInsensitive) {
}
