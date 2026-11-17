package io.vanillabp.cockpit.mongo.changestreams;

import java.util.Optional;
import java.util.UUID;

import org.bson.BsonDocument;
import org.bson.Document;
import org.springframework.data.mongodb.core.query.Update;

import com.mongodb.client.model.changestream.ChangeStreamDocument;

import io.vanillabp.cockpit.mongo.updateinfo.UpdateInformationAware;

/**
 * Who made one change of a user task or a case, written in a way that the change event of MongoDB
 * carries it. The update stream names this person in the event of exactly that change, even if
 * somebody else changed the same entity a moment later, see decision 102 in the repository's
 * DECISIONS.md.
 *
 * <p>A change event of an {@code insert} or a {@code replace} carries the whole document, so it
 * carries the field the entity keeps who made its latest change in. An {@code update} carries only
 * the fields it changed, see decision 31. So every update of a user task or a case also sets the
 * field {@value #FIELD}: who made the change, and an id which is new for every write. MongoDB
 * leaves a field which keeps its value out of the change event. Without the new id, the second of
 * two changes by the same person would arrive without the initiator.
 *
 * <p>Writing the read state of a task is the one update without it, because it is no change of the
 * task for anybody else and produces no event.
 */
public final class LatestChange {

  /** The field of a user task or a case which every update sets. */
  public static final String FIELD = "latestChange";

  /** Who made the change, the same kind of string the entity stores. */
  static final String INITIATOR = "initiator";

  /** New for every write, so the field never keeps its value. */
  static final String CHANGE_ID = "changeId";

  private static final String INSERT = "insert";

  private static final String REPLACE = "replace";

  private static final String UPDATE = "update";

  /** Only static methods, so nobody needs an instance. */
  private LatestChange() {
  }

  /**
   * Adds who made the change to an update of a user task or a case.
   *
   * @param update The update to send
   * @param initiator The id of the user, or {@link UpdateInformationAware#COCKPIT_USER} where the
   *        cockpit made the change without a user
   * @return The same update, for chaining
   */
  public static Update recordIn(
      final Update update,
      final String initiator) {

    return update.set(FIELD, new Document(CHANGE_ID, UUID.randomUUID().toString()).append(INITIATOR, initiator));

  }

  /**
   * Who made the change a change event is about, if the event says it.
   *
   * <p>It says nothing on Azure Cosmos DB for MongoDB, which reports neither the operation nor
   * what an update changed. It says nothing for a deletion, and nothing for an update which did
   * not set {@value #FIELD}. Then the update streams read who made the latest change from the
   * stored entity, which is the best answer left.
   *
   * @param event The change event of MongoDB
   * @param fieldOfTheStoredInitiator The field the entity keeps who made its latest change in,
   *        which an insert or a replace carries in the whole document
   * @return The id of the user or the name the cockpit stores for the system or for itself, or
   *         nothing if the event does not say it
   */
  public static Optional<String> initiatorCarriedBy(
      final ChangeStreamDocument<Document> event,
      final String fieldOfTheStoredInitiator) {

    final var operation = event.getOperationTypeString();
    if ((INSERT.equals(operation) || REPLACE.equals(operation)) && (event.getFullDocument() != null)) {
      return Optional.of(orSystem(event.getFullDocument().getString(fieldOfTheStoredInitiator)));
    }
    if (UPDATE.equals(operation) && (event.getUpdateDescription() != null)) {
      return initiatorOfAnUpdate(event.getUpdateDescription().getUpdatedFields());
    }
    return Optional.empty();

  }

  /**
   * MongoDB reports the field as it was set, as one sub-document. A field path reported for each
   * part of it is accepted as well, so nothing gets lost if a server describes it that way.
   */
  private static Optional<String> initiatorOfAnUpdate(
      final BsonDocument updatedFields) {

    if (updatedFields == null) {
      return Optional.empty();
    }
    final var wholeField = updatedFields.get(FIELD);
    if ((wholeField != null) && wholeField.isDocument()) {
      final var initiator = wholeField.asDocument().get(INITIATOR);
      return Optional.of(orSystem((initiator != null) && initiator.isString()
          ? initiator.asString().getValue()
          : null));
    }
    final var initiatorAlone = updatedFields.get(FIELD
        + "."
        + INITIATOR);
    if ((initiatorAlone != null) && initiatorAlone.isString()) {
      return Optional.of(initiatorAlone.asString().getValue());
    }
    return Optional.empty();

  }

  /**
   * Nothing stored counts as the workflow system, the same way the update streams read it from the
   * stored entity. Only a report can leave it empty.
   */
  private static String orSystem(
      final String stored) {

    return stored == null
        ? UpdateInformationAware.SYSTEM_USER
        : stored;

  }

}
