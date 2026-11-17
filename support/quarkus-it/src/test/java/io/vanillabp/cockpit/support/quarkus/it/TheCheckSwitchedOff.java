package io.vanillabp.cockpit.support.quarkus.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * The application of this module with the check of the cockpit's login token switched off by the
 * build property.
 */
public class TheCheckSwitchedOff implements QuarkusTestProfile {

  @Override
  public Map<String, String> getConfigOverrides() {

    return Map.of("business-cockpit.jwt.enabled", "false");

  }

}
