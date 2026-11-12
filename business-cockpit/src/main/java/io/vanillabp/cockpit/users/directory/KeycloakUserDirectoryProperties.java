package io.vanillabp.cockpit.users.directory;

import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * How the cockpit reaches a Keycloak realm, read from
 * {@code business-cockpit.user-directory.keycloak.*}.
 * <p>
 * The cockpit logs in as a client of the realm, with the client credentials grant of OAuth 2. The
 * client needs "Client authentication" and "Service accounts roles" switched on, and its service
 * account needs the role {@code view-users} of the client {@code realm-management}. Newer releases of
 * Keycloak also need the client scope {@code kc-admin-api} among the default client scopes of the
 * client, because the admin REST API accepts only a token for its own audience.
 *
 * @param serverUrl The address of Keycloak, without the realm, for example
 *          {@code https://login.example.com}
 * @param realm The realm which holds the users
 * @param clientId The id of the client the cockpit logs in with
 * @param clientSecret The secret of that client
 * @param userId Which value of a Keycloak user is the id of the user in the cockpit
 */
public record KeycloakUserDirectoryProperties(
                                              String serverUrl,
                                              String realm,
                                              String clientId,
                                              String clientSecret,
                                              @DefaultValue("username") KeycloakUserId userId) {
}
