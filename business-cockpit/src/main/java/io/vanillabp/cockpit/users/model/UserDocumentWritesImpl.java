package io.vanillabp.cockpit.users.model;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import io.vanillabp.cockpit.notification.model.NotificationConfiguration;

/**
 * The atomic writes of {@link UserDocumentWrites}. Spring Data finds this class by its name and
 * adds its methods to {@link UserRepository}.
 */
class UserDocumentWritesImpl implements UserDocumentWrites {

  private final MongoTemplate mongoTemplate;

  /**
   * Creates the writes. Spring Data calls it.
   *
   * @param mongoTemplate Used to write the user documents
   */
  UserDocumentWritesImpl(
      final MongoTemplate mongoTemplate) {

    this.mongoTemplate = mongoTemplate;

  }

  @Override
  public void writeNotificationConfiguration(
      final String userId,
      final NotificationConfiguration notificationConfiguration) {

    upsert(userId, "notificationConfiguration", notificationConfiguration);

  }

  @Override
  public void writeRecipientConfiguration(
      final String userId,
      final String mediumType,
      final Map<String, String> values) {

    if ((mediumType == null) || mediumType.isEmpty() || mediumType.contains(".") || mediumType.startsWith("$")) {
      throw new IllegalArgumentException("'"
          + mediumType
          + "' cannot be the type of a medium, because it becomes the name of a field in MongoDB");
    }
    upsert(userId, "recipientConfigurations."
        + mediumType, values == null ? new HashMap<>() : new HashMap<>(values));

  }

  private void upsert(
      final String userId,
      final String path,
      final Object value) {

    mongoTemplate.upsert(
        Query.query(Criteria.where("_id").is(userId)),
        new Update()
            .set(path, value)
            .inc("version", 1),
        User.class);

  }

}
