package io.vanillabp.cockpit.support.security.jwt;

import static io.restassured.RestAssured.given;
import static io.vanillabp.cockpit.support.security.jwt.TheSupportOnQuarkus.loginOf;
import static org.hamcrest.Matchers.equalTo;

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
 * The browser sends the cockpit's cookie with every request to a workflow module. The module reads
 * the login token from it, and the request belongs to the user the token names, with the groups the
 * directory gives.
 */
@ExtendWith(SuppressOutputExtension.class)
public class TheCookieLogsTheUserInTest {

  @RegisterExtension
  static final QuarkusExtensionTest application = new QuarkusExtensionTest()
      .withApplicationRoot(jar -> TheSupportOnQuarkus
          .withTheSupport(jar)
          .addClasses(FormResource.class, DirectoryOfTheTest.class))
      .overrideConfigKey(JwtSettings.SECRET_KEY, TheSupportOnQuarkus.SECRET_BASE64);

  @Inject
  DirectoryOfTheTest directory;

  @BeforeEach
  public void setUp() {

    directory.clear();
    directory.with("john", "SALES");

  }

  @Test
  public void theRequestBelongsToTheUserOfTheToken() {

    given()
        .cookie("bc", loginOf("john"))
        .get("/form/free")
        .then()
        .statusCode(200)
        .body(equalTo("john"));

  }

  @Test
  public void theUserHasTheGroupsOfTheDirectoryAsRoles() {

    given()
        .cookie("bc", loginOf("john"))
        .get("/form/roles")
        .then()
        .statusCode(200)
        .body(equalTo("SALES,USER_john"));

    given()
        .cookie("bc", loginOf("john"))
        .get("/form/sales")
        .then()
        .statusCode(200)
        .body(equalTo("sold"));

  }

  @Test
  public void theIdentityCarriesTheCheckedToken() {

    given()
        .cookie("bc", loginOf("john"))
        .get("/form/token")
        .then()
        .statusCode(200)
        .body(equalTo("john"));

  }

  @Test
  public void aUserWithoutTheGroupIsForbidden() {

    directory.with("jane", "CLERK");

    given()
        .cookie("bc", loginOf("jane"))
        .get("/form/sales")
        .then()
        .statusCode(403);

  }

}
