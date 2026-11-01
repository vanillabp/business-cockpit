package io.vanillabp.cockpit.workflowlist.model.changesets;

import java.util.regex.Pattern;

import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import com.phactum.mongodb.changesets.DbChangeset;
import com.phactum.mongodb.changesets.DbChangesetConfiguration;

import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.cockpit.workflowlist.model.WorkflowEndReason;

/**
 * The third database changeset of the workflows. It gives every case which ended before the
 * cockpit stored why it ended a reason, by the rule the cockpit used to follow. And it adds an
 * index on the note, which the full-text search reads beside the reported words.
 */
@Component("V102_Workflow")
@DbChangesetConfiguration(author = "stephanpelikan")
public class V000003 {

  /** A comment with at least one character which is not a space. */
  static final Pattern A_COMMENT_WHICH_SAYS_SOMETHING = Pattern.compile("\\S");

  /** The name of the index on the note. */
  static final String INDEX_NOTE = Workflow.COLLECTION_NAME
      + "_note";

  /** Creates the changeset. The changeset runner finds it as a Spring bean. */
  public V000003() {
  }

  /**
   * An ended case without {@code endReason} counts as cancelled if its {@code comment} says
   * something, and as completed otherwise. A cancellation used to write its reason into
   * {@code comment}, and a completion did not, so this is the only trace there is. The
   * rule lives here and nowhere else, because a completion may carry a comment as well.
   * <p>
   * {@code updatedAt} stays as it is. The case did not change, only what the cockpit knows
   * about it.
   *
   * @param mongo The template to change the database with
   * @return The command which undoes the changeset. No case had {@code endReason} before, so
   *         removing it everywhere is the way back
   */
  @DbChangeset(order = 1016)
  public String giveEveryEndedCaseItsReason(
      final MongoTemplate mongo) {

    mongo.updateMulti(
        new Query(new Criteria().andOperator(
            Criteria.where("endedAt").ne(null),
            Criteria.where("endReason").is(null),
            Criteria.where("comment").regex(A_COMMENT_WHICH_SAYS_SOMETHING))),
        new Update().set("endReason", WorkflowEndReason.CANCELLED.name()),
        Workflow.COLLECTION_NAME);
    mongo.updateMulti(
        new Query(new Criteria().andOperator(
            Criteria.where("endedAt").ne(null),
            Criteria.where("endReason").is(null))),
        new Update().set("endReason", WorkflowEndReason.COMPLETED.name()),
        Workflow.COLLECTION_NAME);

    return "{ update: '"
        + Workflow.COLLECTION_NAME
        + "', updates: [ { q: { endReason: { $exists: true } }, u: { $unset: { endReason: '' } }, multi: true } ] }";

  }

  /**
   * Creates an index on the note. A filter without a path asks for the reported words or the
   * note. MongoDB answers such an "or" from indexes only if each part has one, and the reported
   * words have had theirs since the first changeset.
   *
   * @param mongo The template to change the database with
   * @return The command which drops the index again
   */
  @DbChangeset(order = 1017)
  public String indexTheNote(
      final MongoTemplate mongo) {

    mongo
        .indexOps(Workflow.COLLECTION_NAME)
        .createIndex(new Index()
            .on("note", Direction.ASC)
            .named(INDEX_NOTE));

    return "{ dropIndexes: '"
        + Workflow.COLLECTION_NAME
        + "', index: '"
        + INDEX_NOTE
        + "' }";

  }

}
