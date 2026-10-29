package io.vanillabp.cockpit.commons.rest.adapter;

/**
 * The settings to log in at a Keycloak server. Nothing in this repository uses this class.
 */
public class KeycloakAuthentication extends Client {

  private String clientId;

  /**
   * Creates the settings with their defaults.
   */
  public KeycloakAuthentication() {
  }

  /**
   * The id of the Keycloak client.
   *
   * @return The client id
   */
  public String getClientId() {
    return clientId;
  }

  /**
   * Sets the id of the Keycloak client.
   *
   * @param clientId The client id
   */
  public void setClientId(
      String clientId) {
    this.clientId = clientId;
  }

}
