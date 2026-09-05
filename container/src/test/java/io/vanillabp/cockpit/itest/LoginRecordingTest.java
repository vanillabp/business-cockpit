package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import io.vanillabp.cockpit.users.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Signing in has to leave a {@code users} document behind, because notifications are only sent to
 * users who have been here at least once. The filter doing that is registered globally rather than
 * inside a security filter chain, so that a derived cockpit application replacing the chain
 * inherits the behavior - which also means nothing else in the request path would complain if it
 * silently stopped running.
 */
class LoginRecordingTest extends ItestBase {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    void signingInRecordsTheUser() {

        final var cookie = loginToGui(USER_PETRA);
        assertThat(guiGet(cookie, "/app/current-user").statusCode()).isEqualTo(200);

        final var user = mongoTemplate.findById(USER_PETRA, User.class);
        assertThat(user).isNotNull();
        assertThat(user.getLastLoggedIn()).isNotNull();

    }

}
