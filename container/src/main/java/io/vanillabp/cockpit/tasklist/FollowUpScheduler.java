package io.vanillabp.cockpit.tasklist;

import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.util.events.NotificationEvent;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Emits UserTask FOLLOWUP events for tasks whose follow-up date has
 * elapsed since the last tick, so SSE-connected clients know that the
 * list content may have changed (a previously hidden task can now appear)
 * and re-fetch — even if the task is not in their current list yet.
 *
 * Per-instance state only — when running multiple cluster nodes each node
 * keeps its own {@code previousCheckTimestamp} and emits to its own SSE
 * subscribers, which is sufficient because SSE clients are pinned to a node.
 */
@Component
public class FollowUpScheduler {

    @Autowired
    private Logger logger;

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

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

        mongoTemplate
                .find(query, UserTask.class)
                .doOnNext(task -> {
                    logger.debug(
                            "Follow-up elapsed for user-task '{}' (followUpDate={}, targetGroups={})",
                            task.getId(), task.getFollowUpDate(), task.getTargetGroups());
                    applicationEventPublisher.publishEvent(
                            new UserTaskChangedNotification(
                                    NotificationEvent.Type.FOLLOWUP,
                                    task.getId(),
                                    task.getTargetGroups()));
                })
                .count()
                .doOnNext(count -> {
                    if (count > 0) {
                        logger.info(
                                "Emitted {} user-task FOLLOWUP event(s) for elapsed follow-up dates (window {} → {})",
                                count, since, now);
                    }
                })
                .doOnError(e -> logger.warn(
                        "Could not emit follow-up update events for window {} → {}",
                        since, now, e))
                .onErrorResume(e -> Mono.empty())
                .subscribe();

    }

}
