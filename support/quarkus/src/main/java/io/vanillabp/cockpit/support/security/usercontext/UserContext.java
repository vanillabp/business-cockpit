package io.vanillabp.cockpit.support.security.usercontext;

import java.util.List;

import io.quarkus.security.identity.SecurityIdentity;
import io.vanillabp.cockpit.support.security.BcUnauthorizedException;
import io.vanillabp.cockpit.support.security.jwt.LoginTokenIdentityProvider;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Tells who is logged in for the current request. It has the same name and methods as the class of
 * the Spring Boot module, so the code of a workflow module which uses it moves from one platform to
 * the other unchanged.
 * <p>
 * It reads the {@link SecurityIdentity} of the request. A request without a user, for example
 * without the cockpit's cookie, ends in {@link BcUnauthorizedException}, which this module answers
 * with {@code 401 Unauthorized}.
 * <p>
 * It is there in every application, whatever the login. With the check of the cockpit's login token
 * switched off, it reads the identity of the application's own login. An application without
 * {@code quarkus-security} has no identity at all, so for it nobody is logged in. That is why the
 * identity is looked up and not injected: injected, it would stop the build of an application which
 * has the support only for its exceptions.
 */
@RequestScoped
public class UserContext {

  @Inject
  Instance<SecurityIdentity> identityOfTheRequest;

  /**
   * Creates the context. Quarkus calls this constructor.
   */
  public UserContext() {
  }

  /**
   * The id of the user logged in.
   *
   * @return The user id
   * @throws BcUnauthorizedException If nobody is logged in
   */
  public String getUserLoggedIn() throws BcUnauthorizedException {

    return getUserLoggedInDetails().getId();

  }

  /**
   * The details of the user logged in. For a login by the cockpit's token, the name and the e-mail
   * address come from the directory or the token. For a login of another kind, the application's
   * own, they hold only the name of the user and its roles.
   *
   * @return The details
   * @throws BcUnauthorizedException If nobody is logged in
   */
  public UserDetails getUserLoggedInDetails() throws BcUnauthorizedException {

    if (!identityOfTheRequest.isResolvable()) {
      throw new BcUnauthorizedException("User anonymous");
    }
    final var identity = identityOfTheRequest.get();
    if (identity.isAnonymous()) {
      throw new BcUnauthorizedException("User anonymous");
    }

    final UserDetails fromTheToken = identity.getAttribute(LoginTokenIdentityProvider.USER_DETAILS_ATTRIBUTE);
    if (fromTheToken != null) {
      return fromTheToken;
    }
    return new UserDetailsImpl(
        identity.getPrincipal().getName(), null, null, null, List.copyOf(identity.getRoles()));

  }

}
