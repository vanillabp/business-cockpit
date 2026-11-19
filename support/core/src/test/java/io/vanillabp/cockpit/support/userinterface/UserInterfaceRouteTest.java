package io.vanillabp.cockpit.support.userinterface;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The route takes the files of its workflow module, and nothing else.
 */
@ExtendWith(SuppressOutputExtension.class)
public class UserInterfaceRouteTest {

  private final UserInterfaceRoute route = new UserInterfaceRoute("loan-approval", URI.create("http://nginx:8081"));

  @Test
  public void theFilesOfTheModuleAreForwarded() {

    assertThat(route.forwards("/loan-approval/bc-manifest.json")).isTrue();
    assertThat(route.forwards("/loan-approval/entries/UserTaskForm-1234.js")).isTrue();
    assertThat(route.forwards("/loan-approval/")).isTrue();
    assertThat(route.forwards("/loan-approval/apiary/file.js")).isTrue();

  }

  @Test
  public void theApiOfTheModuleStaysInTheApplication() {

    assertThat(route.forwards("/loan-approval/api")).isFalse();
    assertThat(route.forwards("/loan-approval/api/")).isFalse();
    assertThat(route.forwards("/loan-approval/api/loan-requests/1/assess-risk/2")).isFalse();

  }

  @Test
  public void otherPathsAreNotForwarded() {

    assertThat(route.forwards(null)).isFalse();
    assertThat(route.forwards("/loan-approval")).isFalse();
    assertThat(route.forwards("/loan-approval-2/bc-manifest.json")).isFalse();
    assertThat(route.forwards("/other/bc-manifest.json")).isFalse();
    assertThat(route.forwards("/")).isFalse();

  }

  @Test
  public void aPathWhichCouldLeaveTheModuleIsNotForwarded() {

    assertThat(route.forwards("/loan-approval/../other/secret")).isFalse();
    assertThat(route.forwards("/loan-approval/entries/..")).isFalse();
    assertThat(route.forwards("/loan-approval/./bc-manifest.json")).isFalse();
    assertThat(route.forwards("/loan-approval/%2E%2E/other")).isFalse();
    assertThat(route.forwards("/loan-approval/..%2fother")).isFalse();
    assertThat(route.forwards("/loan-approval/..%5Cother")).isFalse();
    assertThat(route.forwards("/loan-approval/..\\other")).isFalse();

  }

  @Test
  public void theKeyAndTheNameAreBelowTheIdOfTheModule() {

    assertThat(UserInterfaceRoute.keyOf("loan-approval")).isEqualTo("loan-approval.user-interface.address");
    assertThat(route.name()).isEqualTo("loan-approval-user-interface");

  }

  @Test
  public void thePortIsTheDefaultOfTheSchemeUnlessTheAddressNamesOne() {

    assertThat(route.port()).isEqualTo(8081);
    assertThat(route.isTls()).isFalse();
    assertThat(new UserInterfaceRoute("a", URI.create("http://nginx")).port()).isEqualTo(80);
    final var tls = new UserInterfaceRoute("a", URI.create("HTTPS://nginx"));
    assertThat(tls.port()).isEqualTo(443);
    assertThat(tls.isTls()).isTrue();

  }

}
