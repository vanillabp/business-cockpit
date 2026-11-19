package io.vanillabp.cockpit.support.quarkus.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * The application of this module with the user interface of the workflow module
 * {@code loan-approval} in an nginx of its own. The application sets the address, and nothing else.
 */
public class TheUserInterfaceInAnNginx implements QuarkusTestProfile {

  @Override
  public Map<String, String> getConfigOverrides() {

    return Map.of("loan-approval.user-interface.address", EchoingNginx.address());

  }

}
