package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.net.URI;
import java.net.http.HttpRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The container carries no user interface, see decision 63 in the repository's DECISIONS.md. So a
 * path which no API knows, a deep link of a user interface included, gets 404 and not the 500 of
 * the catch-all error handler. An application which puts a shell on its class path gets that shell
 * instead, which {@code SpaNoHandlerFoundExceptionHandlerTest} shows.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class UnknownPathsTest extends ItestBase {

    private java.net.http.HttpResponse<String> get(
            final String path,
            final String cookie) {

        final var request = HttpRequest.newBuilder(URI.create(url(path))).GET();
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        return send(request.header("Accept", "text/html").build());

    }

    @Test
    void anUnknownPathIsNotFound() {

        final var cookie = loginToGui(USER_MARTIN);

        assertThat(get("/tasklist/some-task-id", cookie).statusCode()).isEqualTo(404);
        assertThat(get("/", cookie).statusCode()).isEqualTo(404);

    }

    /**
     * Everything except a handful of endpoints needs a login, unknown paths included. The browser
     * is expected to authenticate and ask again.
     */
    @Test
    void anUnknownPathWithoutAuthenticationIsRejected() {

        assertThat(get("/tasklist/some-task-id", null).statusCode()).isEqualTo(401);

    }

    /**
     * The sign-in request carries basic auth, and its response carries the JWT cookie which the
     * cockpit derives the user from. So that request cannot report a user yet. It answers with an
     * empty body instead of an error, and the client asks again with the cookie.
     */
    @Test
    void theSignInRequestSetsTheCookieAndAnswersWithoutAUser() {

        final var response = send(HttpRequest
                .newBuilder(URI.create(url("/gui/api/v1/app/current-user")))
                .header("Authorization", basicAuth(USER_MARTIN, GUI_PASSWORD))
                .GET()
                .build());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEmpty();
        assertThat(response.headers().allValues("set-cookie"))
                .anyMatch(cookie -> cookie.startsWith("bc="));

    }

}
