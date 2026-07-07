package io.vanillabp.cockpit.users;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.users.model.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Mono;

/**
 * Upserts the {@code users} document of an authenticated user at login time, so the notification
 * feature can restrict recipients to users who have logged in at least once (AC tech 5).
 * <p>
 * Non-blocking and failure-tolerant (a failing upsert never fails the request). The write is an
 * atomic, lock-free Mongo upsert that only touches {@code lastLoggedIn} and {@code email} (existing
 * notification configuration is preserved and the {@code @Version} optimistic lock is not involved),
 * so concurrent requests never conflict. Additionally throttled to at most once per configured
 * window per node to avoid unnecessary writes.
 */
public class UserLoginUpsertService {

    private static final Logger logger = LoggerFactory.getLogger(UserLoginUpsertService.class);

    private final ReactiveMongoTemplate mongoTemplate;

    private final Duration throttle;

    private final Clock clock;

    private final Map<String, Instant> lastUpsertPerUser = new ConcurrentHashMap<>();

    public UserLoginUpsertService(
            final ReactiveMongoTemplate mongoTemplate,
            final Duration throttle,
            final Clock clock) {

        this.mongoTemplate = mongoTemplate;
        this.throttle = throttle;
        this.clock = clock;

    }

    /**
     * Creates or refreshes the {@code users} document for the given authenticated user. Preserves
     * any existing notification configuration; only refreshes {@code lastLoggedIn} and {@code email}.
     */
    public Mono<Void> upsertOnLogin(
            final UserDetails userDetails) {

        if (userDetails == null || userDetails.getId() == null) {
            return Mono.empty();
        }

        final var userId = userDetails.getId();
        final var now = clock.instant();
        final var previous = lastUpsertPerUser.get(userId);
        if (previous != null && Duration.between(previous, now).compareTo(throttle) < 0) {
            return Mono.empty();
        }
        lastUpsertPerUser.put(userId, now);

        // atomic, lock-free upsert: only set lastLoggedIn/email, preserving any existing config and
        // not engaging the @Version optimistic lock, so concurrent requests cannot conflict
        final var query = Query.query(Criteria.where("_id").is(userId));
        final var update = new Update()
                .set("lastLoggedIn", OffsetDateTime.ofInstant(now, ZoneOffset.UTC))
                .set("email", userDetails.getEmail());
        return mongoTemplate
                .upsert(query, update, User.class)
                .then()
                .onErrorResume(e -> {
                    logger.warn("Could not upsert user '{}' on login", userId, e);
                    return Mono.empty();
                });

    }

}
