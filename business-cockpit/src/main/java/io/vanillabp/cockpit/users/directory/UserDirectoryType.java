package io.vanillabp.cockpit.users.directory;

/**
 * The user directories the cockpit brings, chosen by {@code business-cockpit.user-directory.type}.
 */
public enum UserDirectoryType {

  /**
   * The users and groups of a Keycloak realm, read through the admin REST API of Keycloak.
   */
  KEYCLOAK,

  /**
   * The users and groups of an LDAP directory, read with Spring LDAP.
   */
  LDAP

}
