package io.vanillabp.cockpit.support.userinterface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Which routes an application gets, and how a wrong key ends the start with a message which says
 * what to change.
 */
@ExtendWith(SuppressOutputExtension.class)
public class UserInterfaceRoutesTest {

  private static final String ADD_THE_PROXY = "Add the library.";

  @Test
  public void theWorkflowModulesComeFromTheirFiles(
      @TempDir final Path directory) throws IOException {

    final var loanApproval = moduleOf(directory.resolve("one"), "  loan-approval\n");
    final var taxiRide = moduleOf(directory.resolve("two"), "taxi-ride");
    final var empty = moduleOf(directory.resolve("three"), " ");
    try (var classLoader = new URLClassLoader(new URL[]{
        taxiRide, loanApproval, empty
    }, null)) {
      assertThat(UserInterfaceRoutes.workflowModulesOn(classLoader)).containsExactly("loan-approval", "taxi-ride");
    }

  }

  @Test
  public void aModuleGetsARouteIfItsAddressIsSet() {

    final var keys = Map.of("loan-approval.user-interface.address", " http://nginx:8081/ ");

    final var routes = UserInterfaceRoutes.of(
        List.of("taxi-ride", "loan-approval"), key -> Optional.ofNullable(keys.get(key)), keys.keySet(), true,
        ADD_THE_PROXY);

    assertThat(routes).containsExactly(new UserInterfaceRoute("loan-approval", URI.create("http://nginx:8081/")));

  }

  @Test
  public void withoutAnAddressThereIsNoRouteAndTheProxyIsNotNeeded() {

    final var keys = Map.of("loan-approval.user-interface.address", "");

    assertThat(UserInterfaceRoutes.of(
        List.of("loan-approval"), key -> Optional.ofNullable(keys.get(key)), keys.keySet(), false, ADD_THE_PROXY))
        .isEmpty();

  }

  @Test
  public void aRouteWithoutTheProxyEndsTheStart() {

    final var keys = Map.of("loan-approval.user-interface.address", "http://nginx:8081");

    assertThatThrownBy(() -> UserInterfaceRoutes.of(
        List.of("loan-approval"), key -> Optional.ofNullable(keys.get(key)), keys.keySet(), false, ADD_THE_PROXY))
        .isInstanceOf(UserInterfaceRoutesMisconfigured.class)
        .hasMessageContaining("'loan-approval.user-interface.address' asks for a route to the user interface, but "
            + "the application has no library which passes a request on. Add the library.");

  }

  @Test
  public void everyWrongAddressIsNamedAtOnce() {

    final var keys = Map.of(
        "a.user-interface.address", "nginx:8081",
        "b.user-interface.address", "http://nginx:8081/loan-approval",
        "c.user-interface.address", "http://user:secret@nginx:8081",
        "d.user-interface.address", "http://nginx :8081",
        "e.user-interface.address", "ftp://nginx",
        "f.user-interface.address", "http://nginx:8081?x=1");

    assertThatThrownBy(() -> UserInterfaceRoutes.of(
        List.of("a", "b", "c", "d", "e", "f"), key -> Optional.ofNullable(keys.get(key)), keys.keySet(), true,
        ADD_THE_PROXY))
        .isInstanceOfSatisfying(UserInterfaceRoutesMisconfigured.class, failure -> assertThat(failure.getProblems())
            .hasSize(6))
        .hasMessageStartingWith("The routes to the user interfaces of the workflow modules need a change:")
        .hasMessageContaining("'a.user-interface.address' is 'nginx:8081', but it needs the scheme, the host")
        .hasMessageContaining("'b.user-interface.address' is 'http://nginx:8081/loan-approval', but only its "
            + "scheme, host and port count.")
        .hasMessageContaining("'c.user-interface.address' holds a user")
        .hasMessageContaining("'d.user-interface.address' is no address")
        .hasMessageContaining("'e.user-interface.address' is 'ftp://nginx'")
        .hasMessageContaining("'f.user-interface.address' is 'http://nginx:8081?x=1', but only its");

  }

  @Test
  public void anAddressOfAModuleTheApplicationDoesNotHaveEndsTheStart() {

    final var keys = Map.of(
        "loan-aproval.user-interface.address", "http://nginx:8081",
        "%dev.taxi.user-interface.address", "http://nginx:8081",
        "loan-approval.cockpit.base-url", "http://cockpit");

    assertThatThrownBy(() -> UserInterfaceRoutes.of(
        List.of("loan-approval"), key -> Optional.ofNullable(keys.get(key)), keys.keySet(), true, ADD_THE_PROXY))
        .isInstanceOfSatisfying(UserInterfaceRoutesMisconfigured.class, failure -> assertThat(failure.getProblems())
            .hasSize(2))
        .hasMessageContaining("'loan-aproval.user-interface.address' names no workflow module of this application, "
            + "so there is no route for it. The workflow modules of the application are: loan-approval.")
        .hasMessageContaining("'%dev.taxi.user-interface.address' names no workflow module");

  }

  @Test
  public void anApplicationWithoutModulesSaysSo() {

    final var keys = Map.of("loan-approval.user-interface.address", "http://nginx:8081");

    assertThatThrownBy(() -> UserInterfaceRoutes.of(
        List.of(), key -> Optional.ofNullable(keys.get(key)), keys.keySet(), true, ADD_THE_PROXY))
        .hasMessageContaining("The application has no workflow module with a file META-INF/workflow-module.");

  }

  private static URL moduleOf(
      final Path root,
      final String content) throws IOException {

    final var file = root.resolve(UserInterfaceRoutes.WORKFLOW_MODULE_FILE);
    Files.createDirectories(file.getParent());
    Files.writeString(file, content, StandardCharsets.UTF_8);
    return root.toUri().toURL();

  }

}
