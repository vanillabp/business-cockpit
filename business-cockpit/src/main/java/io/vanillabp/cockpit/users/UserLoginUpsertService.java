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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Upserts the {@code users} document of an authenticated user at login, so the notification
 * feature can restrict the recipients to users who have logged in at least once.
 * <p>
 * An upsert which fails never fails the request. The write is one atomic MongoDB upsert without a
 * lock, and it touches only {@code lastLoggedIn} and {@code email}. It keeps whatever
 * notification configuration is stored and never reads the {@code @Version} optimistic lock, so
 * two requests at once do not conflict. On top of that, a node writes at most once per configured
 * window.
 */
public class UserLoginUpsertService {

    private static final Logger logger = LoggerFactory.getLogger(UserLoginUpsertService.class);

    private final MongoTemplate mongoTemplate;

    private final Duration throttle;

    private final Clock clock;

    private final Map<String, Instant> lastUpsertPerUser = new ConcurrentHashMap<>();

    public UserLoginUpsertService(
            final MongoTemplate mongoTemplate,
            final Duration throttle,
            final Clock clock) {

        this.mongoTemplate = mongoTemplate;
        this.throttle = throttle;
        this.clock = clock;

    }

    /**
     * Creates or refreshes the {@code users} document of the given authenticated user. It keeps
     * whatever notification configuration is stored and refreshes {@code lastLoggedIn} and
     * {@code email} only.
     */
    public void upsertOnLogin(
            final UserDetails userDetails) {

        if (userDetails == null || userDetails.getId() == null) {
            return;
        }

        final var userId = userDetails.getId();
        final var now = clock.instant();
        final var previous = lastUpsertPerUser.get(userId);
        if (previous != null && Duration.between(previous, now).compareTo(throttle) < 0) {
            return;
        }
        lastUpsertPerUser.put(userId, now);

        // atomic, lock-free upsert: only set lastLoggedIn/email, preserving any existing config and
        // not engaging the @Version optimistic lock, so concurrent requests cannot conflict
        final var query = Query.query(Criteria.where("_id").is(userId));
        final var update = new Update()
                .set("lastLoggedIn", OffsetDateTime.ofInstant(now, ZoneOffset.UTC))
                .set("email", userDetails.getEmail());
        try {
            mongoTemplate.upsert(query, update, User.class);
        } catch (Exception e) {
            logger.warn("Could not upsert user '{}' on login", userId, e);
        }

    }

}
