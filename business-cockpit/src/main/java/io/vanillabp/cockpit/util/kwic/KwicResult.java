package io.vanillabp.cockpit.util.kwic;

import org.springframework.data.annotation.Id;

/**
 * One word found by a search for suggestions (KWIC, "keyword in context"), and how often it was
 * found.
 *
 * @param item The word found
 * @param count How many times it was found
 */
public record KwicResult(@Id String item, int count) {
}
