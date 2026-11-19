package io.vanillabp.cockpit.support.test;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import io.quarkus.arc.All;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.test.security.TestIdentityAssociation;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Logs a user in for the calls of a test, the way the login of an application would. A test of a
 * workflow module calls the beans of the module as different users, one after the other, and the
 * security of the module decides as it does for a real request.
 *
 * <pre>
 * &#64;Inject
 * TestLogin login;
 *
 * &#64;Test
 * void onlyAnAssessorAssessesTheRisk() {
 *   login.logIn("bob", "LOAN_APPROVAL_ASSESS_RISK");
 *   ... // calls of the module as bob
 *   login.logIn("mallory");
 *   ... // calls of the module as mallory, who has no role
 * }
 * </pre>
 *
 * <p>
 * {@code TestIdentityAssociation} of {@code quarkus-test-security} sets the user of a test, but
 * it takes the identity as it is. A login of the application runs every
 * {@link SecurityIdentityAugmentor} of the application on the identity, and Quarkus brings the
 * permission checkers of a module ({@code @PermissionChecker}) to the identity that way. So this
 * class runs them as well, the one with the highest priority first. Without it, every check of a
 * permission refuses the call.
 * </p>
 *
 * <p>
 * The roles are the ones the test names, together with what the augmentors add. A group hierarchy
 * of the cockpit's login, the {@code GroupResolver} of {@code business-cockpit-quarkus-support},
 * is not applied, because a login of the application's own does not apply it either. A test which
 * needs the derived groups names them.
 * </p>
 *
 * <p>
 * The user holds for the whole application until the next {@link #logIn} or {@link #logOut()}, not
 * only for the thread of the test: {@code TestIdentityAssociation} keeps one user for all. A request
 * which a test sends over HTTP with a login of its own, the cockpit's cookie or the login of the
 * application, keeps that login. A request without a login runs as the user of the test. So a test
 * class logs out after each test, and the next test class starts without a user:
 * </p>
 *
 * <pre>
 * &#64;AfterEach
 * void logOut() {
 *   login.logOut();
 * }
 * </pre>
 */
@ApplicationScoped
public class TestLogin {

  @Inject
  TestIdentityAssociation identities;

  @Inject
  @All
  List<SecurityIdentityAugmentor> augmentors;

  /**
   * Creates the bean. Quarkus calls this constructor.
   */
  public TestLogin() {
  }

  /**
   * Logs the user in with the roles, for the calls which follow.
   *
   * @param user  The id of the user
   * @param roles The roles of the user, none for a user who may do nothing but log in
   * @return The identity the calls now run as, with what the augmentors added
   */
  public SecurityIdentity logIn(
      final String user,
      final String... roles) {

    return logIn(user, Set.of(roles));

  }

  /**
   * Logs the user in with the roles, for the calls which follow.
   *
   * @param user  The id of the user
   * @param roles The roles of the user
   * @return The identity the calls now run as, with what the augmentors added
   */
  public SecurityIdentity logIn(
      final String user,
      final Collection<String> roles) {

    final SecurityIdentity loggedIn = QuarkusSecurityIdentity
        .builder()
        .setPrincipal(new QuarkusPrincipal(user))
        .addRoles(Set.copyOf(roles))
        .build();
    final var augmented = augmented(loggedIn);
    identities.setTestIdentity(augmented);
    return augmented;

  }

  /**
   * Logs the user out. The calls which follow run without a user, like a request without a login.
   */
  public void logOut() {

    identities.setTestIdentity(null);

  }

  private SecurityIdentity augmented(
      final SecurityIdentity loggedIn) {

    // the augmentors run here, in the thread of the test, and wait for nothing else
    final AuthenticationRequestContext inThisThread = blocking -> Uni
        .createFrom()
        .item(blocking);
    var identity = loggedIn;
    for (final var augmentor : augmentors
        .stream()
        .sorted(Comparator.comparingInt(SecurityIdentityAugmentor::priority).reversed())
        .toList()) {
      identity = augmentor
          .augment(identity, inThisThread)
          .await()
          .indefinitely();
    }
    return identity;

  }

}
