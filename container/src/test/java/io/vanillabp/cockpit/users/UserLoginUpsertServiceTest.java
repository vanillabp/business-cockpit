package io.vanillabp.cockpit.users;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mongodb.client.result.UpdateResult;
import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.users.model.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

class UserLoginUpsertServiceTest {

    /** A Clock whose instant can be advanced from the test. */
    private static final class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-07-07T10:00:00Z");

        @Override
        public Instant instant() {
            return instant;
        }

        void advance(Duration by) {
            instant = instant.plus(by);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    private MongoTemplate mongoTemplate;
    private MutableClock clock;
    private UserLoginUpsertService service;

    private static UserDetails userDetails(String id, String email) {
        return new UserDetails() {
            public String getId() {
                return id;
            }

            public String getEmail() {
                return email;
            }

            public String getDisplay() {
                return id;
            }

            public String getDisplayShort() {
                return id;
            }

            public List<String> getAuthorities() {
                return List.of();
            }
        };
    }

    @BeforeEach
    void setUp() {
        mongoTemplate = mock(MongoTemplate.class);
        clock = new MutableClock();
        service = new UserLoginUpsertService(mongoTemplate, Duration.ofMinutes(5), clock);
        when(mongoTemplate.upsert(any(Query.class), any(Update.class), eq(User.class)))
                .thenReturn(mock(UpdateResult.class));
    }

    @Test
    void firstLogin_upsertsOnce() {
        service.upsertOnLogin(userDetails("u1", "u1@example.org"));

        verify(mongoTemplate, times(1)).upsert(any(Query.class), any(Update.class), eq(User.class));
    }

    @Test
    void secondLoginWithinThrottleWindow_doesNotWriteAgain() {
        service.upsertOnLogin(userDetails("u1", "a@b.c"));
        clock.advance(Duration.ofMinutes(2));
        service.upsertOnLogin(userDetails("u1", "a@b.c"));

        verify(mongoTemplate, times(1)).upsert(any(Query.class), any(Update.class), eq(User.class));
    }

    @Test
    void loginAfterThrottleWindow_writesAgain() {
        service.upsertOnLogin(userDetails("u1", "a@b.c"));
        clock.advance(Duration.ofMinutes(6));
        service.upsertOnLogin(userDetails("u1", "a@b.c"));

        verify(mongoTemplate, times(2)).upsert(any(Query.class), any(Update.class), eq(User.class));
    }

    @Test
    void throttleIsPerUser() {
        service.upsertOnLogin(userDetails("u1", "a@b.c"));
        service.upsertOnLogin(userDetails("u2", "d@e.f"));

        verify(mongoTemplate, times(2)).upsert(any(Query.class), any(Update.class), eq(User.class));
    }

    @Test
    void upsertFailure_doesNotFailRequest() {
        Mockito.doThrow(new RuntimeException("mongo down"))
                .when(mongoTemplate).upsert(any(Query.class), any(Update.class), eq(User.class));

        // must not throw (error swallowed) so the request is never broken
        service.upsertOnLogin(userDetails("u1", "a@b.c"));
    }

    @Test
    void nullDetailsOrId_isNoOp() {
        service.upsertOnLogin(null);
        service.upsertOnLogin(userDetails(null, "a@b.c"));
        verify(mongoTemplate, never()).upsert(any(Query.class), any(Update.class), eq(User.class));
    }

}
