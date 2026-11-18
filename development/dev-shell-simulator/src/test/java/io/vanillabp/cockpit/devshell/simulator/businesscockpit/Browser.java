package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Calls the simulator the way a user interface in a browser does: it keeps the login cookie. The
 * user is chosen the way the dev shell chooses him, by {@code POST /dev-shell/user/{userId}}.
 */
final class Browser {

  private final HttpClient client = HttpClient
      .newBuilder()
      .cookieHandler(new CookieManager())
      .build();

  private final int port;

  /**
   * Creates a browser with nobody signed in.
   *
   * @param port The port of the simulator
   */
  Browser(
      final int port) {

    this.port = port;

  }

  /**
   * Signs a user in, as the dev shell does when the developer picks him.
   *
   * @param userId The id of the user
   * @return The browser, with the login cookie of the user
   * @throws Exception If the simulator cannot be reached
   */
  Browser signIn(
      final String userId) throws Exception {

    send("POST", "/dev-shell/user/"
        + userId, "");
    return this;

  }

  /**
   * Sends a request.
   *
   * @param method The HTTP method
   * @param path The path, starting with a slash
   * @param body The JSON body, or {@code null} for none
   * @return The answer, with the body as text
   * @throws Exception If the simulator cannot be reached
   */
  HttpResponse<String> send(
      final String method,
      final String path,
      final String body) throws Exception {

    final var request = HttpRequest
        .newBuilder(URI.create("http://localhost:"
            + port
            + path))
        .header("Content-Type", "application/json")
        .method(method, body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(body))
        .build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());

  }

  /**
   * Sends a GET request.
   *
   * @param path The path, starting with a slash
   * @return The answer, with the body as text
   * @throws Exception If the simulator cannot be reached
   */
  HttpResponse<String> get(
      final String path) throws Exception {

    return send("GET", path, null);

  }

}
