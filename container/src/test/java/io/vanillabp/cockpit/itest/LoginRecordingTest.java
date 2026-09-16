package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import io.vanillabp.cockpit.users.model.User;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Signing in has to leave a {@code users} document behind, because notifications are only sent to
 * users who have been here at least once. The filter which does that is registered globally and
 * not inside a security filter chain, so a derived cockpit application which replaces the chain
 * inherits the behavior. It also means that nothing else in the request path would complain if the
 * filter stopped running.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
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
