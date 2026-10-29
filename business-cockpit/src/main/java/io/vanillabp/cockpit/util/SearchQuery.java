package io.vanillabp.cockpit.util;

/**
 * One filter of a list request.
 *
 * @param path The field to filter on. Without a path, the full text is searched.
 * @param query The regular expression the field has to match
 * @param caseInsensitive Whether the match ignores case
 */
public record SearchQuery(String path, String query, boolean caseInsensitive) {
}
