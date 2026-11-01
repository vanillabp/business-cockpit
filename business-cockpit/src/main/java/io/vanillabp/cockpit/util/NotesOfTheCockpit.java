package io.vanillabp.cockpit.util;

import java.time.OffsetDateTime;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.StringUtils;

/**
 * How the cockpit writes the note of a user task or a case. The note belongs to the cockpit, and
 * a report of a workflow module never carries it.
 * <p>
 * A report is stored by reading the document, laying the report onto it and saving it whole.
 * Writing the note the same way would lose it whenever a report arrives in between. So the note
 * is written by an update of its own, and the update raises {@code version}. A report which read
 * the document before is then refused by the version check and goes through again later, with
 * the note in it.
 */
public final class NotesOfTheCockpit {

  /** The field which holds the note, in a user task and in a case. */
  public static final String FIELD = "note";

  /** Only static methods, so nobody needs an instance. */
  private NotesOfTheCockpit() {
  }

  /**
   * The update which sets a note, or removes it where the text is empty.
   *
   * @param text The text of the note, or {@code null}
   * @param userId The user who writes it
   * @return The update
   */
  public static Update setTo(
      final String text,
      final String userId) {

    final var update = new Update();
    if (StringUtils.hasText(text)) {
      update.set(FIELD, text);
    } else {
      update.unset(FIELD);
    }
    update.inc("version", 1);
    update.set("updatedAt", OffsetDateTime.now());
    update.set("updatedBy", userId);
    return update;

  }

  /**
   * The condition which keeps an entry out of the update once it has ended. It closes the gap
   * between the check for the end and the write.
   *
   * @return The condition
   */
  public static CriteriaDefinition notEnded() {
    return Criteria.where("endedAt").exists(false);
  }

}
