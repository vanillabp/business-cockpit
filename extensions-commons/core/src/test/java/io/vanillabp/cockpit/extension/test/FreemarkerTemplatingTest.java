package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.templating.FreemarkerTemplating;
import io.vanillabp.cockpit.extension.templating.Templating;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Where templates are loaded from.
 * <p>
 * A workflow module ships its templates inside its jar as often as it points at a directory of
 * the file system, and version 1 accepted both spellings. Both are asserted here, because a
 * location which silently loads nothing looks exactly like a workflow module without templates.
 */
@ExtendWith(SuppressOutputExtension.class)
public class FreemarkerTemplatingTest {

  private static final Map<String, Object> ORDER = Map
      .of("order", new EventTitlesTest.Order("4711", 250));

  /** Where the templates of this module lie while the tests run. */
  private static Path templateDirectory() {

    return Path.of("src", "test", "resources", "templates").toAbsolutePath();

  }

  private static String rendered(
      final Templating templating) {

    return templating
        .render(
            List.of("test-module/TestProcess/approve", "test-module"),
            Templating.TASK_TITLE, Locale.ENGLISH, ORDER, Map.of())
        .orElseThrow(() -> new AssertionError("no template was found"));

  }

  @Test
  @DisplayName("Templates are loaded from the classpath")
  public void templatesAreLoadedFromTheClasspath() {

    assertEquals(
        "Approve order 4711",
        rendered(new FreemarkerTemplating("classpath:templates")));

  }

  @Test
  @DisplayName("The classpath spelling of version 1 loads the same templates")
  public void theClasspathSpellingOfVersionOneWorks() {

    assertEquals(
        "Approve order 4711",
        rendered(new FreemarkerTemplating("classpath*:/templates/")));

  }

  @Test
  @DisplayName("Templates are loaded from a directory, written with and without 'file:'")
  public void templatesAreLoadedFromADirectory() {

    assertEquals(
        "Approve order 4711",
        rendered(new FreemarkerTemplating(templateDirectory().toString())));
    assertEquals(
        "Approve order 4711",
        rendered(new FreemarkerTemplating("file:"
            + templateDirectory())));

  }

  @Test
  @DisplayName("A directory which does not exist is refused with a message naming it")
  public void aMissingDirectoryIsRefused() {

    final var failure = assertThrows(
        IllegalStateException.class,
        () -> new FreemarkerTemplating(templateDirectory().resolve("nowhere").toString()));

    assertTrue(failure.getMessage().contains("nowhere"), failure.getMessage());

  }

}
