package io.vanillabp.cockpit.users;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.bson.BsonMaximumSizeExceededException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import com.mongodb.MongoException;

import io.vanillabp.cockpit.commons.exceptions.BcContentTooLargeException;
import io.vanillabp.cockpit.commons.exceptions.BcInvalidRequestException;
import io.vanillabp.cockpit.users.model.User;

/**
 * Reads and writes the settings the user interface keeps for a user, in the field
 * {@code customSettings} of the user's document.
 * <p>
 * Each write touches one key and nothing else. It is one atomic update, {@code $set} or
 * {@code $unset} of {@code customSettings.<key>}, and it creates the document if the user has
 * none yet. Reading the whole document, changing it and saving it again would let two tabs
 * overwrite each other, and the notification configuration with them.
 * <p>
 * The write also raises {@code version}, as every write of one part of the user's document does
 * (see {@code UserDocumentWrites}). A save of the whole document which read it before a setting
 * was written then fails the version check, so it cannot wipe out the new setting.
 */
@Component
public class UserSettings {

  /** The field of the user document which holds the settings. */
  public static final String FIELD = "customSettings";

  /**
   * What a key may hold. It becomes the name of a field in MongoDB, which reads a dot as a step
   * into a nested document and a leading {@code $} as an operator. Letters, digits and three
   * signs cover every name the user interface needs.
   */
  private static final Pattern KEY = Pattern.compile("[A-Za-z0-9_:-]{1,200}");

  /**
   * MongoDB refuses a document larger than 16 MB with one of these codes: 10334 when it is
   * written whole, 17419 when an update makes it grow too large.
   */
  private static final int[] CODES_OF_A_DOCUMENT_TOO_LARGE = {
      10334, 17419
  };

  private final MongoTemplate mongoTemplate;

  /**
   * Creates the service.
   *
   * @param mongoTemplate Used to read and write the user documents
   */
  public UserSettings(
      final MongoTemplate mongoTemplate) {

    this.mongoTemplate = mongoTemplate;

  }

  /**
   * Reads one setting.
   *
   * @param userId The user the setting belongs to
   * @param key The name of the setting
   * @return The value, or empty if the user has no setting of this key
   * @throws BcInvalidRequestException If the key breaks the rule of {@link #KEY}
   */
  public Optional<Object> get(
      final String userId,
      final String key) {

    final var path = pathOf(key);
    final var query = Query.query(Criteria.where("_id").is(userId));
    query.fields().include(path);
    return Optional
        .ofNullable(mongoTemplate.findOne(query, User.class))
        .map(User::getCustomSettings)
        .filter(settings -> settings.containsKey(key))
        .map(settings -> settings.get(key));

  }

  /**
   * Writes one setting and leaves all other settings as they are.
   *
   * @param userId The user the setting belongs to
   * @param key The name of the setting
   * @param value The value, which is the JSON the user interface sent, as Jackson read it
   * @throws BcInvalidRequestException If the key breaks the rule of {@link #KEY}, if the value
   *         is {@code null}, or if an object in the value has a key with a dot or a leading
   *         {@code $}
   * @throws BcContentTooLargeException If the user's document would get larger than MongoDB
   *         allows
   */
  public void set(
      final String userId,
      final String key,
      final Object value) {

    final var path = pathOf(key);
    if (value == null) {
      // a stored null would be a third state next to "set" and "not there", and a GET could not
      // tell it from a missing body. Removing has a request of its own
      throw new BcInvalidRequestException(
          "the value of '%s' is null, use DELETE to remove a setting".formatted(key));
    }

    final var badKey = aKeyMongoDbCannotTake(value);
    if (badKey.isPresent()) {
      throw new BcInvalidRequestException(
          "an object in the value of '%s' has the key '%s', but a key inside a setting may not hold a dot or start with '$'"
              .formatted(key, badKey.get()));
    }

    final var update = new Update()
        .set(path, value)
        .inc("version", 1);
    try {
      mongoTemplate.upsert(Query.query(Criteria.where("_id").is(userId)), update, User.class);
    } catch (RuntimeException e) {
      if (isDocumentTooLarge(e)) {
        throw new BcContentTooLargeException(
            "The setting '%s' is not stored. All settings of a user share one MongoDB document, and it would be larger than 16 MB."
                .formatted(key));
      }
      throw e;
    }

  }

  /**
   * MongoDB stores a key with a dot, or one which starts with {@code $}, when it comes in by an
   * update. But the mapping of Spring Data refuses a key with a dot in a map when it writes a whole
   * document. No code saves the user's document whole: each part of it is written by an update of
   * its own. The rule keeps a save of the whole document possible, because one such key in a
   * setting would make every such save of this user's document fail.
   *
   * @param value A value as Jackson read it: maps, lists and plain values
   * @return The first key which cannot be taken, if there is one
   */
  private static Optional<String> aKeyMongoDbCannotTake(
      final Object value) {

    if (value instanceof Map<?, ?> map) {
      for (final var entry : map.entrySet()) {
        final var key = String.valueOf(entry.getKey());
        if (key.contains(".") || key.startsWith("$") || key.indexOf('\0') >= 0) {
          return Optional.of(key);
        }
        final var nested = aKeyMongoDbCannotTake(entry.getValue());
        if (nested.isPresent()) {
          return nested;
        }
      }
    } else if (value instanceof Collection<?> collection) {
      for (final var element : collection) {
        final var nested = aKeyMongoDbCannotTake(element);
        if (nested.isPresent()) {
          return nested;
        }
      }
    }
    return Optional.empty();

  }

  /**
   * Removes one setting and leaves all other settings as they are. Removing a setting which is
   * not there is no mistake.
   *
   * @param userId The user the setting belongs to
   * @param key The name of the setting
   * @throws BcInvalidRequestException If the key breaks the rule of {@link #KEY}
   */
  public void remove(
      final String userId,
      final String key) {

    final var path = pathOf(key);
    // only a document which holds the key is changed, so a second DELETE raises no version
    mongoTemplate.updateFirst(
        Query.query(Criteria.where("_id").is(userId).and(path).exists(true)),
        new Update()
            .unset(path)
            .inc("version", 1),
        User.class);

  }

  private static String pathOf(
      final String key) {

    if ((key == null) || !KEY.matcher(key).matches()) {
      throw new BcInvalidRequestException(
          "'key' takes 1 to 200 letters, digits, '-', '_' and ':'");
    }
    return FIELD
        + "."
        + key;

  }

  private static boolean isDocumentTooLarge(
      final Throwable exception) {

    for (var cause = exception; cause != null; cause = cause.getCause()) {
      // the driver refuses an update which is too large by itself, before MongoDB sees it
      if (cause instanceof BsonMaximumSizeExceededException) {
        return true;
      }
    }
    for (final var code : CODES_OF_A_DOCUMENT_TOO_LARGE) {
      if (hasCode(exception, code)) {
        return true;
      }
    }
    return false;

  }

  private static boolean hasCode(
      final Throwable exception,
      final int code) {

    for (var cause = exception; cause != null; cause = cause.getCause()) {
      if ((cause instanceof MongoException mongoException) && (mongoException.getCode() == code)) {
        return true;
      }
    }
    return false;

  }

}
