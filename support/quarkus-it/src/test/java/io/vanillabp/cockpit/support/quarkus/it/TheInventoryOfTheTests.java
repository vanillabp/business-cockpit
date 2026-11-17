package io.vanillabp.cockpit.support.quarkus.it;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Sets the keys of the client of the tests before the application starts. The service and the
 * OAuth server are played by a stub server which the test starts itself, inside the application,
 * on the port chosen here. The test framework loads the test class in another class loader than
 * this resource, so the port travels as a system property.
 */
public class TheInventoryOfTheTests implements QuarkusTestResourceLifecycleManager {

  /** The system property which holds the port of the stub server. */
  public static final String PORT = "support-quarkus-it.inventory.port";

  @Override
  public Map<String, String> start() {

    final int port;
    try (var socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    System.setProperty(PORT, Integer.toString(port));
    final var stub = "http://127.0.0.1:"
        + port;
    return Map.of(
        "loan-approval.inventory.base-url", stub
            + "/api",
        "loan-approval.inventory.oauth.token-uri", stub
            + "/oauth/token",
        "loan-approval.inventory.oauth.client-id", "loan-approval",
        "loan-approval.inventory.oauth.client-secret", "s3cret");

  }

  @Override
  public void stop() {
    System.clearProperty(PORT);
  }

}
