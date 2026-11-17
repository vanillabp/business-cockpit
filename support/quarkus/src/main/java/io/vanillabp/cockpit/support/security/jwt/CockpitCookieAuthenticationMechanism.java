package io.vanillabp.cockpit.support.security.jwt;

import java.util.Set;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Reads the login token from the cookie the Business Cockpit writes, and lets
 * {@link LoginTokenIdentityProvider} tell the user. The browser sends the cookie with every request
 * to a workflow module behind the cockpit, because both answer under the same host.
 * <p>
 * It is passive: it never answers a request on its own. A request without the cookie, or with a
 * token which is not valid, goes on without a user, and the security rules of the endpoint decide
 * what it may do. An endpoint which needs a user answers {@code 401 Unauthorized}, one which needs a
 * group the user does not have answers {@code 403 Forbidden}. It sends no challenge either, because
 * the login happens in the cockpit and not here.
 * <p>
 * A user directory which fails is no invalid token. Then the request fails, see
 * {@link io.vanillabp.cockpit.support.security.usercontext.DirectoryUserLookup}.
 * <p>
 * The build property {@value JwtSettings#ENABLED_KEY}{@code =false} leaves it out of the
 * application.
 */
@ApplicationScoped
@IfBuildProperty(name = JwtSettings.ENABLED_KEY, stringValue = "true", enableIfMissing = true)
public class CockpitCookieAuthenticationMechanism implements HttpAuthenticationMechanism {

  @Inject
  JwtSettings settings;

  /**
   * Creates the mechanism. Quarkus calls this constructor.
   */
  public CockpitCookieAuthenticationMechanism() {
  }

  @Override
  public Uni<SecurityIdentity> authenticate(
      final RoutingContext context,
      final IdentityProviderManager identityProviderManager) {

    final var cookie = context
        .request()
        .getCookie(settings.cookieName());
    if ((cookie == null) || cookie.getValue().isBlank()) {
      return Uni.createFrom().nullItem();
    }

    final var request = HttpSecurityUtils.setRoutingContextAttribute(
        new LoginTokenAuthenticationRequest(cookie.getValue()), context);
    return identityProviderManager
        .authenticate(request)
        .onFailure(AuthenticationFailedException.class)
        .recoverWithNull();

  }

  @Override
  public Uni<ChallengeData> getChallenge(
      final RoutingContext context) {

    return Uni.createFrom().nullItem();

  }

  @Override
  public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {

    return Set.of(LoginTokenAuthenticationRequest.class);

  }

  @Override
  public Uni<HttpCredentialTransport> getCredentialTransport(
      final RoutingContext context) {

    return Uni.createFrom().item(
        new HttpCredentialTransport(HttpCredentialTransport.Type.COOKIE, settings.cookieName()));

  }

}
