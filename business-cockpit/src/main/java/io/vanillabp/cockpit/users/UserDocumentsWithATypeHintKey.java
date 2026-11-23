package io.vanillabp.cockpit.users;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import io.vanillabp.cockpit.users.model.TypeHintKey;
import io.vanillabp.cockpit.users.model.User;

/**
 * Looks once at startup for user documents which hold the key {@value TypeHintKey#KEY} in a
 * part a user writes: the settings, the notification configuration and the recipient
 * configurations. The cockpit refuses that key now, but a document written before can still hold
 * it, and Spring Data cannot read such a document. The user then gets no notifications. The
 * check changes nothing. It logs a warning which names each such user and the field, so an
 * operator can remove the key.
 */
@Component
public class UserDocumentsWithATypeHintKey {

  private static final Logger logger = LoggerFactory.getLogger(UserDocumentsWithATypeHintKey.class);

  /** The parts of the user document a user writes free keys into. */
  static final List<String> FIELDS_A_USER_WRITES = List.of(
      UserSettings.FIELD, "notificationConfiguration", "recipientConfigurations");

  private final MongoTemplate mongoTemplate;

  /**
   * Creates the check.
   *
   * @param mongoTemplate Used to read the user documents as they are stored
   */
  public UserDocumentsWithATypeHintKey(
      final MongoTemplate mongoTemplate) {

    this.mongoTemplate = mongoTemplate;

  }

  /**
   * Runs the check once the application is ready. A failure is logged and does not stop the
   * start, because the check only reports.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void reportAtStartup() {

    try {
      final var found = find();
      if (!found.isEmpty()) {
        logger.warn("""
            {} user document(s) hold the key '{}' in a field a user writes: {}. \
            Spring Data cannot read such a document, so these users get no notifications. \
            Remove the key in MongoDB, for example with an $unset of the path in the collection '{}'.""",
            found.size(), TypeHintKey.KEY, found, User.COLLECTION_NAME);
      }
    } catch (RuntimeException e) {
      logger.warn("Could not check the user documents for the key '{}'", TypeHintKey.KEY, e);
    }

  }

  /**
   * Reads the stored documents and changes nothing.
   *
   * @return Each user and field which holds the key, like {@code "mallory (customSettings)"}
   */
  public List<String> find() {

    final var query = new Query();
    FIELDS_A_USER_WRITES.forEach(query.fields()::include);
    final var found = new ArrayList<String>();
    for (final var document : mongoTemplate.find(query, Document.class, User.COLLECTION_NAME)) {
      for (final var field : FIELDS_A_USER_WRITES) {
        if (TypeHintKey.isIn(document.get(field))) {
          found.add("%s (%s)".formatted(document.get("_id"), field));
        }
      }
    }
    return found;

  }

}
