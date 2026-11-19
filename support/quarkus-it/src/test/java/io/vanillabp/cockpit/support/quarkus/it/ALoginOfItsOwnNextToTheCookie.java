package io.vanillabp.cockpit.support.quarkus.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * The application of this module with a login of its own, HTTP basic authentication with a user in
 * the configuration, next to the check of the cockpit's cookie. The check stays on.
 */
public class ALoginOfItsOwnNextToTheCookie implements QuarkusTestProfile {

  @Override
  public Map<String, String> getConfigOverrides() {

    return Map.of(
        "quarkus.http.auth.basic", "true",
        "quarkus.security.users.embedded.enabled", "true",
        "quarkus.security.users.embedded.plain-text", "true",
        "quarkus.security.users.embedded.users.ann", "secret",
        "quarkus.security.users.embedded.roles.ann", "CLERK");

  }

}
