package io.vanillabp.cockpit.support.security.jwt;

import java.time.Clock;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vanillabp.cockpit.support.security.usercontext.DirectoryUserLookup;
import io.vanillabp.cockpit.support.security.usercontext.GroupResolver;
import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Tells the user of a login token of the Business Cockpit. It checks the token with
 * {@link LoginTokenVerifier} and asks the application's {@link UserDirectoryService} for the user
 * and its groups with {@link DirectoryUserLookup}, the same two a Spring Boot workflow module uses.
 * The groups the directory gives go through the {@link GroupResolver}, and the result becomes the
 * roles of the {@link SecurityIdentity}. So {@code @RolesAllowed("SALES")} on an endpoint means: the
 * directory gives the user the group {@code SALES}, or a group which implies it.
 * <p>
 * A token which is not valid gives no identity, and the request goes on without a user. A directory
 * which fails fails the request. The directory may ask a remote system, so it is asked off the I/O
 * thread.
 * <p>
 * The identity carries the user as {@link io.vanillabp.cockpit.support.security.usercontext.UserDetails}
 * in the attribute {@value #USER_DETAILS_ATTRIBUTE}, and the checked token in the attribute
 * {@value #LOGIN_TOKEN_ATTRIBUTE}. {@link io.vanillabp.cockpit.support.security.usercontext.UserContext}
 * reads the first.
 */
@ApplicationScoped
@IfBuildProperty(name = JwtSettings.ENABLED_KEY, stringValue = "true", enableIfMissing = true)
public class LoginTokenIdentityProvider implements IdentityProvider<LoginTokenAuthenticationRequest> {

  /**
   * The attribute of the identity which holds the user as {@code UserDetails}.
   */
  public static final String USER_DETAILS_ATTRIBUTE = "io.vanillabp.cockpit.user-details";

  /**
   * The attribute of the identity which holds the checked {@link LoginToken}.
   */
  public static final String LOGIN_TOKEN_ATTRIBUTE = "io.vanillabp.cockpit.login-token";

  private static final Logger logger = LoggerFactory.getLogger(LoginTokenIdentityProvider.class);

  @Inject
  JwtSettings settings;

  @Inject
  Instance<UserDirectoryService> userDirectory;

  @Inject
  GroupResolver groupResolver;

  private volatile LoginTokenVerifier verifier;

  private volatile DirectoryUserLookup userLookup;

  /**
   * Creates the provider. Quarkus calls this constructor.
   */
  public LoginTokenIdentityProvider() {
  }

  @Override
  public Class<LoginTokenAuthenticationRequest> getRequestType() {

    return LoginTokenAuthenticationRequest.class;

  }

  @Override
  public Uni<SecurityIdentity> authenticate(
      final LoginTokenAuthenticationRequest request,
      final AuthenticationRequestContext context) {

    return context.runBlocking(() -> identityOf(request.getToken()));

  }

  private SecurityIdentity identityOf(
      final String token) {

    final LoginToken checked;
    try {
      checked = verifier().verify(token);
    } catch (InvalidLoginTokenException e) {
      if (e.isExpired()) {
        logger.debug("The login token is not valid, so the request goes on without a user: {}", e.getMessage());
      } else {
        logger.warn("The login token is not valid, so the request goes on without a user: {}", e.getMessage());
      }
      throw new AuthenticationFailedException(e.getMessage(), e);
    }

    final var user = userLookup().lookUp(checked.subject());

    return QuarkusSecurityIdentity
        .builder()
        .setPrincipal(new QuarkusPrincipal(user.id()))
        .addRoles(user.authorities())
        .addAttribute(USER_DETAILS_ATTRIBUTE, new TokenUserDetails(checked, user))
        .addAttribute(LOGIN_TOKEN_ATTRIBUTE, checked)
        .build();

  }

  private LoginTokenVerifier verifier() {

    if (verifier == null) {
      final var secret = settings
          .hmacSha256Base64()
          .orElseThrow(() -> new IllegalStateException(
              LoginTokenVerifier.explainTheMissingSecret(JwtSettings.SECRET_KEY)));
      verifier = new LoginTokenVerifier(Base64.getDecoder().decode(secret));
    }
    return verifier;

  }

  private DirectoryUserLookup userLookup() {

    if (userLookup == null) {
      userLookup = new DirectoryUserLookup(userDirectory.get(), groupResolver::resolve, logger, Clock.systemUTC());
    }
    return userLookup;

  }

}
