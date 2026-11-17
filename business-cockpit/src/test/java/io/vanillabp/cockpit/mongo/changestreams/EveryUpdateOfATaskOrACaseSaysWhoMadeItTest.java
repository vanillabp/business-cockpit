package io.vanillabp.cockpit.mongo.changestreams;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Every update of a user task or a case sets {@link LatestChange}, so its change event says who
 * made it. An update which forgets it still produces an event, but the event names who made the
 * latest change instead of who made this one, and no other test notices it, see decision 102 in
 * the repository's DECISIONS.md.
 *
 * <p>Writes which save the whole document need nothing of this kind. Their change event carries
 * the whole document, and with it who made the change. So only the updates are checked here, in
 * the sources: each statement which sends an update to the collection of the user tasks or of the
 * cases has to call {@link LatestChange#recordIn} itself. The one exception is the read state of
 * a task, which is no change of the task for anybody else and produces no event. The changesets
 * run before the cockpit opens its change streams, so they are left out.
 */
@ExtendWith(SuppressOutputExtension.class)
class EveryUpdateOfATaskOrACaseSaysWhoMadeItTest {

  private static final Path MAIN_SOURCES = Path.of("src", "main", "java");

  private static final Pattern AN_UPDATE = Pattern.compile(
      "\\.(update|updateFirst|updateMulti|updateOne|updateMany|findAndModify|upsert|bulkOps)\\(");

  private static final Pattern A_TASK_OR_A_CASE = Pattern.compile(
      "\\b(UserTask|Workflow)\\.(class|COLLECTION_NAME)\\b");

  @Test
  @DisplayName("Every update of a user task or a case records who made it in the change event")
  void everyUpdateRecordsWhoMadeIt() {

    final var updates = updatesOfATaskOrACase();

    assertThat(updates)
        .as("the scan finds the updates the cockpit is known to send, so it still works")
        .anySatisfy(update -> assertThat(update).contains("FIELD_READ_BY"))
        .anySatisfy(update -> assertThat(update).contains("NotesOfTheCockpit.setTo"));
    assertThat(updates)
        .as("updates of a user task or a case which do not call LatestChange.recordIn")
        .allSatisfy(update -> assertThat(
            update.contains("LatestChange.recordIn(") || isTheReadState(update)).as(update).isTrue());

  }

  /** Having read a task is no change of it for anybody else, so it produces no event. */
  private static boolean isTheReadState(
      final String update) {

    return update.contains("FIELD_READ_BY");

  }

  private static List<String> updatesOfATaskOrACase() {

    try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
      return files
          .filter(file -> file.toString().endsWith(".java"))
          .filter(file -> !file.toString().contains("changesets"))
          .flatMap(file -> statementsSendingAnUpdate(read(file)).stream())
          .filter(statement -> A_TASK_OR_A_CASE.matcher(statement).find())
          .toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

  }

  private static String read(
      final Path file) {

    try {
      return Files.readString(file);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

  }

  /**
   * The statements which call one of the update methods: from the end of the statement before to
   * the closing parenthesis of the call.
   */
  private static List<String> statementsSendingAnUpdate(
      final String source) {

    final var statements = new ArrayList<String>();
    final var matcher = AN_UPDATE.matcher(source);
    while (matcher.find()) {
      final var start = Math.max(
          source.lastIndexOf(';', matcher.start()),
          Math.max(source.lastIndexOf('{', matcher.start()), source.lastIndexOf('}', matcher.start()))) + 1;
      var depth = 0;
      var end = matcher.end() - 1;
      do {
        final var character = source.charAt(end);
        if (character == '(') {
          depth++;
        } else if (character == ')') {
          depth--;
        }
        end++;
      } while ((depth > 0) && (end < source.length()));
      statements.add(source.substring(start, end).strip());
    }
    return statements;

  }

}
