package io.vanillabp.cockpit.tasklist.model.changesets;

import java.util.regex.Pattern;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import com.phactum.mongodb.changesets.DbChangeset;
import com.phactum.mongodb.changesets.DbChangesetConfiguration;

import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskEndReason;

/**
 * The third database changeset of the user tasks. It gives every task which ended without a
 * reason a reason, by the rule the cockpit used to follow. Tasks have had {@code endReason} since
 * 0.4.0, so only tasks which ended before are concerned.
 */
@Component("V102_UserTask")
@DbChangesetConfiguration(author = "stephanpelikan")
public class V000003 {

  /** A comment with at least one character which is not a space. */
  static final Pattern A_COMMENT_WHICH_SAYS_SOMETHING = Pattern.compile("\\S");

  /** Creates the changeset. The changeset runner finds it as a Spring bean. */
  public V000003() {
  }

  /**
   * An ended task without {@code endReason} counts as cancelled if its {@code comment} says
   * something, and as completed otherwise. A cancellation used to write its reason into
   * {@code comment}, and a completion did not, so this is the only trace there is. The
   * rule lives here and nowhere else, because a completion may carry a comment as well. A task which has a
   * reason keeps it.
   * <p>
   * {@code updatedAt} stays as it is. The notification poller reads the tasks changed since its
   * last run by {@code updatedAt}, and an old task must not look like one which just ended.
   *
   * @param mongo The template to change the database with
   * @return {@code null}, because the changeset cannot be undone. Removing {@code endReason}
   *         would also remove it from every task which has ended since 0.4.0
   */
  @DbChangeset(order = 17)
  public String giveEveryEndedTaskItsReason(
      final MongoTemplate mongo) {

    mongo.updateMulti(
        new Query(new Criteria().andOperator(
            Criteria.where("endedAt").ne(null),
            Criteria.where("endReason").is(null),
            Criteria.where("comment").regex(A_COMMENT_WHICH_SAYS_SOMETHING))),
        new Update().set("endReason", UserTaskEndReason.CANCELLED.name()),
        UserTask.COLLECTION_NAME);
    mongo.updateMulti(
        new Query(new Criteria().andOperator(
            Criteria.where("endedAt").ne(null),
            Criteria.where("endReason").is(null))),
        new Update().set("endReason", UserTaskEndReason.COMPLETED.name()),
        UserTask.COLLECTION_NAME);

    return null;

  }

}
