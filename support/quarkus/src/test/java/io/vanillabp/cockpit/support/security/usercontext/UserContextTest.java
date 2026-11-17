package io.vanillabp.cockpit.support.security.usercontext;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.smallrye.mutiny.Uni;
import io.vanillabp.cockpit.support.security.jwt.JwtSettings;
import io.vanillabp.cockpit.support.security.jwt.TheSupportOnQuarkus;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * The user context tells the code of a workflow module who is logged in, with the same methods as
 * on Spring Boot. Nobody logged in is {@code 401}. A login of the application's own kind, here by a
 * header, gives the name of the user and its roles.
 */
@ExtendWith(SuppressOutputExtension.class)
public class UserContextTest {

  /**
   * A login of the application's own, by the header {@code X-User}.
   */
  @ApplicationScoped
  public static class HeaderLogin implements HttpAuthenticationMechanism {

    @Override
    public Uni<SecurityIdentity> authenticate(
        final RoutingContext context,
        final IdentityProviderManager identityProviderManager) {

      final var user = context.request().getHeader("X-User");
      if (user == null) {
        return Uni.createFrom().nullItem();
      }
      return Uni.createFrom().item(QuarkusSecurityIdentity
          .builder()
          .setPrincipal(new QuarkusPrincipal(user))
          .addRole("OPERATOR")
          .build());

    }

    @Override
    public Uni<ChallengeData> getChallenge(
        final RoutingContext context) {

      return Uni.createFrom().nullItem();

    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {

      return Set.of();

    }

  }

  @RegisterExtension
  static final QuarkusExtensionTest application = new QuarkusExtensionTest()
      .withApplicationRoot(jar -> TheSupportOnQuarkus
          .withTheSupport(jar)
          .addClasses(FormResource.class, DirectoryOfTheTest.class, HeaderLogin.class))
      .overrideConfigKey(JwtSettings.SECRET_KEY, TheSupportOnQuarkus.SECRET_BASE64);

  @Inject
  DirectoryOfTheTest directory;

  @BeforeEach
  public void setUp() {

    directory.clear();
    directory.with(new UserDetailsImpl("john", "john@example.com", "Doe, John", "Doe, J.", java.util.List.of(
        "SALES")));

  }

  @Test
  public void theUserOfTheCockpitsLoginIsTheUserLoggedIn() {

    given()
        .cookie("bc", TheSupportOnQuarkus.loginOf("john"))
        .get("/form/who")
        .then()
        .statusCode(200)
        .body(equalTo("john"));

    given()
        .cookie("bc", TheSupportOnQuarkus.loginOf("john"))
        .get("/form/details")
        .then()
        .statusCode(200)
        .body(equalTo("john\nDoe, John\nDoe, J.\njohn@example.com\nSALES,USER_john"));

  }

  @Test
  public void nobodyLoggedInIsUnauthorized() {

    given()
        .get("/form/who")
        .then()
        .statusCode(401)
        .body(equalTo("User anonymous"));

    given()
        .get("/form/details")
        .then()
        .statusCode(401)
        .body(equalTo("User anonymous"));

  }

  @Test
  public void aLoginOfTheApplicationsOwnKindGivesTheNameAndTheRoles() {

    given()
        .header("X-User", "operator")
        .get("/form/details")
        .then()
        .statusCode(200)
        .body(equalTo("operator\nnull\nnull\nnull\nOPERATOR"));

  }

}
