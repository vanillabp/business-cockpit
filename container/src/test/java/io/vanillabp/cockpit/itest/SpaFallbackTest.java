package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpRequest;
import org.junit.jupiter.api.Test;

/**
 * A deep link into the single-page application is a path only the browser-side router knows. The
 * server has to answer it with the application shell instead of an error, otherwise reloading the
 * page or opening a bookmark lands on a 404.
 *
 * <p>The shell used here is the stand-in in {@code src/test/resources/static/index.html}; the real
 * one is produced by the webapp build, which this build does not run.
 */
class SpaFallbackTest extends ItestBase {

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
    void deepLinksAreAnsweredWithTheApplicationShell() {

        final var cookie = loginToGui(USER_MARTIN);

        final var deepLink = get("/tasklist/some-task-id", cookie);
        assertThat(deepLink.statusCode()).isEqualTo(200);
        assertThat(deepLink.body()).contains("id=\"root\"");

        final var root = get("/", cookie);
        assertThat(root.statusCode()).isEqualTo(200);
        assertThat(root.body()).contains("id=\"root\"");

    }

    /**
     * Everything except a handful of endpoints needs a login, deep links included. The browser is
     * expected to authenticate and ask again.
     */
    @Test
    void deepLinksWithoutAuthenticationAreRejected() {

        assertThat(get("/tasklist/some-task-id", null).statusCode()).isEqualTo(401);

    }

    /**
     * The sign-in request carries basic auth, and its response carries the JWT cookie the cockpit
     * derives the user from - so that very request cannot report a user yet. It answers with an
     * empty body rather than an error, and the client asks again with the cookie.
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
