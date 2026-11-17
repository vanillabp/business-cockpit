package io.vanillabp.cockpit.support.security.jwt;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.support.security.usercontext.DirectoryOfTheTest;
import io.vanillabp.cockpit.support.security.usercontext.FormResource;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.inject.Inject;

/**
 * A request without the cookie, or with a token which is not valid, goes on without a user. The
 * mechanism never answers on its own and asks for no login, because the login happens in the
 * cockpit. An open endpoint answers, one which needs a user answers {@code 401}. The application
 * names its cookie differently here, and a cookie of the default name is not read.
 */
@ExtendWith(SuppressOutputExtension.class)
public class ARequestWithoutAValidTokenIsAnonymousTest {

  private static final String COOKIE = "cockpit-login";

  @RegisterExtension
  static final QuarkusExtensionTest application = new QuarkusExtensionTest()
      .withApplicationRoot(jar -> TheSupportOnQuarkus
          .withTheSupport(jar)
          .addClasses(FormResource.class, DirectoryOfTheTest.class))
      .overrideConfigKey(JwtSettings.SECRET_KEY, TheSupportOnQuarkus.SECRET_BASE64)
      .overrideConfigKey(JwtSettings.COOKIE_NAME_KEY, COOKIE);

  @Inject
  DirectoryOfTheTest directory;

  @BeforeEach
  public void setUp() {

    directory.clear();
    directory.with("john", "SALES");

  }

  @Test
  public void theCookieOfTheConfiguredNameLogsTheUserIn() {

    given()
        .cookie(COOKIE, TheSupportOnQuarkus.loginOf("john"))
        .get("/form/free")
        .then()
        .statusCode(200)
        .body(equalTo("john"));

  }

  @Test
  public void withoutTheCookieTheRequestIsAnonymous() {

    given()
        .get("/form/free")
        .then()
        .statusCode(200)
        .body(equalTo("anonymous"));

    given()
        .cookie("bc", TheSupportOnQuarkus.loginOf("john"))
        .get("/form/free")
        .then()
        .statusCode(200)
        .body(equalTo("anonymous"));

  }

  @Test
  public void anAnonymousRequestToAnEndpointWhichNeedsAUserIsUnauthorized() {

    given()
        .get("/form/sales")
        .then()
        .statusCode(401);

    given()
        .get("/form/who")
        .then()
        .statusCode(401)
        .body(equalTo("User anonymous"));

  }

  @Test
  public void aTokenSignedWithAnotherSecretIsAnonymous() {

    assertAnonymous(LoginTokens.signed(LoginTokens.newSecret(), "john"));

  }

  @Test
  public void anExpiredTokenIsAnonymous() {

    assertAnonymous(LoginTokens.signed(TheSupportOnQuarkus.secret(), "john", claims -> claims
        .expirationTime(Date.from(Instant.now().minus(Duration.ofMinutes(5))))));

  }

  @Test
  public void textWhichIsNoTokenIsAnonymous() {

    assertAnonymous("no-token");
    assertAnonymous(" ");

  }

  private static void assertAnonymous(
      final String token) {

    given()
        .cookie(COOKIE, token)
        .get("/form/free")
        .then()
        .statusCode(200)
        .body(equalTo("anonymous"));

    given()
        .cookie(COOKIE, token)
        .get("/form/sales")
        .then()
        .statusCode(401);

  }

}
