package io.vanillabp.cockpit.gui.api.v1.client.quarkus;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Sets the keys of the clients of the tests before the application starts. The cockpit is played
 * by a stub server which the test starts itself, inside the application, on the port chosen here.
 * The test framework loads the test class in another class loader than this resource, so the port
 * travels as a system property.
 */
public class TheCockpitOfTheTests implements QuarkusTestResourceLifecycleManager {

  /** The system property which holds the port of the stub server. */
  public static final String PORT = "official-gui-api-quarkus-client.cockpit.port";

  @Override
  public Map<String, String> start() {

    final int port;
    try (var socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    System.setProperty(PORT, Integer.toString(port));
    final var cockpit = "http://127.0.0.1:"
        + port;
    return Map.of(
        // the address of the cockpit only, the clients add the path of the API themselves
        "loan-approval.cockpit.base-url", cockpit
            + "/",
        // a cockpit with a context path
        "onboarding.cockpit.base-url", cockpit
            + "/onboarding",
        // the mistake the start refuses: the path of the API in the address
        "with-the-path.cockpit.base-url", cockpit
            + "/gui/api/v1",
        "onboarding.cockpit.read-timeout", "300ms",
        "another-service.base-url", cockpit
            + "/another-service");

  }

  @Override
  public void stop() {
    System.clearProperty(PORT);
  }

}
