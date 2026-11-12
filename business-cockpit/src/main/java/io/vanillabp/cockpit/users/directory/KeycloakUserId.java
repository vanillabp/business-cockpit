package io.vanillabp.cockpit.users.directory;

/**
 * Which value of a Keycloak user the cockpit uses as the id of the user. It has to be the value the
 * login names the user by, because that value is the subject of the cockpit's login token.
 */
public enum KeycloakUserId {

  /**
   * The user name, which is the claim {@code preferred_username} of a Keycloak token. Choose it if
   * the login is configured with {@code user-name-attribute: preferred_username}.
   */
  USERNAME,

  /**
   * The internal id Keycloak gives every user, which is the claim {@code sub} of a Keycloak token.
   * Spring Security names a user by this claim if nothing else is configured.
   */
  ID

}
