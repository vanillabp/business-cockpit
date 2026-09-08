package io.vanillabp.cockpit.extension.config;

/**
 * Where the cockpit server's BPMS API is and how to authenticate against it.
 *
 * @param baseUrl The address the API is served under, without the API's own path
 * @param username The user of the basic authentication, or <code>null</code> where the server
 *          expects none
 * @param password The password belonging to the user
 */
public record RestTransportConfiguration(
                                         String baseUrl,
                                         String username,
                                         String password) {

  /**
   * @return Whether a user was configured, i.e. whether requests carry an Authorization header
   */
  public boolean authenticates() {

    return (username != null) && !username.isBlank();

  }

}
