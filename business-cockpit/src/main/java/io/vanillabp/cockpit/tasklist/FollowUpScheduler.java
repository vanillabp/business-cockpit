package io.vanillabp.cockpit.tasklist;

import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.util.events.NotificationEvent;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sends a FOLLOWUP event for every user task whose follow-up date has passed since the last tick.
 * A browser on an open event stream then knows that the content of its list may have changed, and
 * it asks again. A task which was hidden until now can appear that way, even though it is not in
 * the list the browser holds.
 * <p>
 * The state belongs to one node. With several nodes in a cluster, each one keeps its own
 * {@code previousCheckTimestamp} and sends to the browsers subscribed to it. That is enough,
 * because a browser stays on the node its event stream was opened on.
 */
@Component
public class FollowUpScheduler {

    @Autowired
    private Logger logger;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    private OffsetDateTime previousCheckTimestamp = OffsetDateTime.now();

    @Scheduled(
            fixedRateString = "${businesscockpit.follow-up.check-rate:PT1M}",
            initialDelayString = "${businesscockpit.follow-up.check-rate:PT1M}")
    public void emitFollowUpReminders() {

        final var now = OffsetDateTime.now();
        final var since = previousCheckTimestamp;
        previousCheckTimestamp = now;

        logger.debug("Follow-up tick window {} → {}", since, now);

        final var query = Query.query(new Criteria().andOperator(
                Criteria.where("endedAt").exists(false),
                Criteria.where("followUpDate").gt(since),
                Criteria.where("followUpDate").lte(now)));

        try {
            final var elapsed = mongoTemplate.find(query, UserTask.class);
            elapsed.forEach(task -> {
                logger.debug(
                        "Follow-up elapsed for user-task '{}' (followUpDate={}, targetGroups={})",
                        task.getId(), task.getFollowUpDate(), task.getTargetGroups());
                applicationEventPublisher.publishEvent(
                        new UserTaskChangedNotification(
                                NotificationEvent.Type.FOLLOWUP,
                                task.getId(),
                                task.getTargetGroups()));
            });
            if (!elapsed.isEmpty()) {
                logger.info(
                        "Emitted {} user-task FOLLOWUP event(s) for elapsed follow-up dates (window {} \u2192 {})",
                        elapsed.size(), since, now);
            }
        } catch (Exception e) {
            logger.warn(
                    "Could not emit follow-up update events for window {} \u2192 {}",
                    since, now, e);
        }

    }

}
