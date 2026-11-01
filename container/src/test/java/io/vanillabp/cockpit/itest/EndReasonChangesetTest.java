package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Tasks and cases which ended before the cockpit stored why have no {@code endReason}. A
 * changeset gives them one on the next start: cancelled where the comment says something,
 * completed otherwise. This test stores documents of that old shape and drives each changeset
 * against the running MongoDB. No request to the application could do it, because changesets
 * run once while the context starts.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class EndReasonChangesetTest extends ItestBase {

  private static final Date ENDED_AT = new Date(1_700_000_000_000L);

  private static final Date UPDATED_AT = new Date(1_690_000_000_000L);

  @Autowired
  private MongoTemplate mongo;

  @Autowired
  private io.vanillabp.cockpit.tasklist.model.changesets.V000003 userTaskChangesets;

  @Autowired
  private io.vanillabp.cockpit.workflowlist.model.changesets.V000003 workflowChangesets;

  private final List<String> stored = new ArrayList<>();

  /**
   * The documents have none of the fields the lists read, so they are removed again before
   * another test class asks a list for everything.
   */
  @AfterEach
  void removeTheOldDocuments() {
    List.of(UserTask.COLLECTION_NAME, Workflow.COLLECTION_NAME).forEach(collection -> mongo
        .remove(new Query(Criteria.where("_id").in(stored)), collection));
  }

  @Test
  void anEndedTaskGetsTheReasonItsCommentSuggests() {

    final var withComment = old(UserTask.COLLECTION_NAME, ENDED_AT, "customer withdrew", null);
    final var withoutComment = old(UserTask.COLLECTION_NAME, ENDED_AT, null, null);
    final var emptyComment = old(UserTask.COLLECTION_NAME, ENDED_AT, "", null);
    final var blankComment = old(UserTask.COLLECTION_NAME, ENDED_AT, "  \t ", null);
    final var open = old(UserTask.COLLECTION_NAME, null, "still waiting", null);
    final var withReason = old(UserTask.COLLECTION_NAME, ENDED_AT, "done, with a remark", "COMPLETED");

    userTaskChangesets.giveEveryEndedTaskItsReason(mongo);

    assertThat(endReasonOf(withComment, UserTask.COLLECTION_NAME)).isEqualTo("CANCELLED");
    assertThat(endReasonOf(withoutComment, UserTask.COLLECTION_NAME)).isEqualTo("COMPLETED");
    assertThat(endReasonOf(emptyComment, UserTask.COLLECTION_NAME)).isEqualTo("COMPLETED");
    assertThat(endReasonOf(blankComment, UserTask.COLLECTION_NAME)).isEqualTo("COMPLETED");
    assertThat(endReasonOf(open, UserTask.COLLECTION_NAME)).isNull();
    assertThat(endReasonOf(withReason, UserTask.COLLECTION_NAME))
        .as("a task which knows its reason keeps it")
        .isEqualTo("COMPLETED");

  }

  /**
   * The notification poller reads the tasks whose {@code updatedAt} moved since its last run. An
   * old task which the changeset touched must not look like a task which has just ended.
   */
  @Test
  void anOldTaskDoesNotLookChanged() {

    final var id = old(UserTask.COLLECTION_NAME, ENDED_AT, "customer withdrew", null);

    userTaskChangesets.giveEveryEndedTaskItsReason(mongo);

    assertThat(stored(id, UserTask.COLLECTION_NAME).get("updatedAt")).isEqualTo(UPDATED_AT);

  }

  @Test
  void anEndedCaseGetsTheReasonItsCommentSuggests() {

    final var withComment = old(Workflow.COLLECTION_NAME, ENDED_AT, "customer withdrew", null);
    final var withoutComment = old(Workflow.COLLECTION_NAME, ENDED_AT, null, null);
    final var emptyComment = old(Workflow.COLLECTION_NAME, ENDED_AT, "", null);
    final var blankComment = old(Workflow.COLLECTION_NAME, ENDED_AT, "   ", null);
    final var open = old(Workflow.COLLECTION_NAME, null, "still running", null);
    final var withReason = old(Workflow.COLLECTION_NAME, ENDED_AT, "done, with a remark", "COMPLETED");

    workflowChangesets.giveEveryEndedCaseItsReason(mongo);

    assertThat(endReasonOf(withComment, Workflow.COLLECTION_NAME)).isEqualTo("CANCELLED");
    assertThat(endReasonOf(withoutComment, Workflow.COLLECTION_NAME)).isEqualTo("COMPLETED");
    assertThat(endReasonOf(emptyComment, Workflow.COLLECTION_NAME)).isEqualTo("COMPLETED");
    assertThat(endReasonOf(blankComment, Workflow.COLLECTION_NAME)).isEqualTo("COMPLETED");
    assertThat(endReasonOf(open, Workflow.COLLECTION_NAME)).isNull();
    assertThat(endReasonOf(withReason, Workflow.COLLECTION_NAME))
        .as("a case which knows its reason keeps it")
        .isEqualTo("COMPLETED");
    assertThat(stored(withComment, Workflow.COLLECTION_NAME).get("updatedAt"))
        .isEqualTo(UPDATED_AT);

  }

  /** Stores a document in the shape the cockpit wrote before it kept the reason of an end. */
  private String old(
      final String collection,
      final Date endedAt,
      final String comment,
      final String endReason) {

    final var id = unique("old");
    final var document = new Document("_id", id)
        .append("createdAt", new Date(1_680_000_000_000L))
        .append("updatedAt", UPDATED_AT);
    if (endedAt != null) {
      document.append("endedAt", endedAt);
    }
    if (comment != null) {
      document.append("comment", comment);
    }
    if (endReason != null) {
      document.append("endReason", endReason);
    }
    mongo.insert(document, collection);
    stored.add(id);
    return id;

  }

  private Document stored(
      final String id,
      final String collection) {
    return mongo.findById(id, Document.class, collection);
  }

  private String endReasonOf(
      final String id,
      final String collection) {
    return stored(id, collection).getString("endReason");
  }

}
