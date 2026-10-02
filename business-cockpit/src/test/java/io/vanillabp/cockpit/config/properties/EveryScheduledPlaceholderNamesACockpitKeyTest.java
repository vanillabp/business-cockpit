package io.vanillabp.cockpit.config.properties;

import static org.assertj.core.api.Assertions.assertThat;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * An interval of a {@code @Scheduled} method is named by a placeholder, and a placeholder is looked
 * up by its exact name. Spring's relaxed binding does not reach it. A key written in camel case or
 * without a dash is therefore a key of its own: nothing sets it, and the default of the annotation
 * is what counts, without a word in the log.
 *
 * <p>Both mistakes were in the tree. The collector of the update stream asked for
 * {@code businessCockpit.guiSse.collectingInterval}, the follow-up scheduler for
 * {@code businesscockpit.follow-up.check-rate}, while the keys of this application live under
 * {@code business-cockpit}. This test is what notices the next one.
 */
@ExtendWith(SuppressOutputExtension.class)
class EveryScheduledPlaceholderNamesACockpitKeyTest {

    private static final String COCKPIT_PACKAGE = "io.vanillabp.cockpit";

    /** The prefix of every configuration key of the cockpit application itself. */
    private static final String PREFIX = "business-cockpit.";

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^:}]+)");

    /** Lower case words, joined by dashes inside a part and by dots between parts. */
    private static final Pattern KEY = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*(\\.[a-z0-9]+(-[a-z0-9]+)*)*");

    @Test
    @DisplayName("Every @Scheduled placeholder of the cockpit names a key below business-cockpit")
    void placeholdersNameConfiguredKeys() {

        final var keys = scheduledPlaceholderKeys();

        assertThat(keys)
                .as("No @Scheduled placeholder found at all, so this test proves nothing")
                .isNotEmpty();

        assertThat(keys)
                .allSatisfy(key -> assertThat(key)
                        .as("'%s' is no key of this application", key)
                        .startsWith(PREFIX)
                        .matches(KEY));

    }

    private Set<String> scheduledPlaceholderKeys() {

        final TypeFilter carriesAScheduledMethod = (reader, factory) -> reader
                .getAnnotationMetadata()
                .hasAnnotatedMethods(Scheduled.class.getName());

        final var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(carriesAScheduledMethod);

        final var keys = new TreeSet<String>();
        scanner
                .findCandidateComponents(COCKPIT_PACKAGE)
                .stream()
                .filter(AnnotatedBeanDefinition.class::isInstance)
                .map(AnnotatedBeanDefinition.class::cast)
                .flatMap(definition -> definition
                        .getMetadata()
                        .getAnnotatedMethods(Scheduled.class.getName())
                        .stream())
                .map(method -> method.getAnnotationAttributes(Scheduled.class.getName()))
                .filter(attributes -> attributes != null)
                .flatMap(attributes -> attributes.values().stream())
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .forEach(value -> {
                    final var placeholders = PLACEHOLDER.matcher(value);
                    while (placeholders.find()) {
                        keys.add(placeholders.group(1));
                    }
                });

        return keys;

    }

}
