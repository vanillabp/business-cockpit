package io.vanillabp.cockpit.notification;

import java.util.Map;

/**
 * A single, medium-specific configuration value of a recipient (e.g. the e-mail address used
 * by the e-mail medium).
 *
 * @param type        an identifier interpreted by the medium implementation (e.g. {@code emailAddress})
 * @param title       the label of the value per language (key = language, value = label)
 * @param description an explanation of the value per language (key = language, value = explanation);
 *                    typically shown as an on-mouse-over hint in the UI
 * @param value       the current value
 */
public record RecipientConfiguration(
        String type,
        Map<String, String> title,
        Map<String, String> description,
        String value) {

}
