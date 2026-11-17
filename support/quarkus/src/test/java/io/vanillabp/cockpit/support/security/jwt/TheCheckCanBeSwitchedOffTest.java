package io.vanillabp.cockpit.support.security.jwt;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.support.security.usercontext.FormResource;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * An application which does not read the cockpit's login token, for example one which only throws
 * the exceptions of a user task, switches the check off with a build property. It then starts
 * without a secret and without a directory, and the cookie is not read.
 */
@ExtendWith(SuppressOutputExtension.class)
public class TheCheckCanBeSwitchedOffTest {

  @RegisterExtension
  static final QuarkusExtensionTest application = new QuarkusExtensionTest()
      .withApplicationRoot(jar -> TheSupportOnQuarkus
          .withTheSupport(jar)
          .addClasses(FormResource.class))
      .overrideConfigKey(JwtSettings.ENABLED_KEY, "false");

  @Test
  public void theApplicationStartsAndDoesNotReadTheCookie() {

    given()
        .cookie("bc", TheSupportOnQuarkus.loginOf("john"))
        .get("/form/free")
        .then()
        .statusCode(200)
        .body(equalTo("anonymous"));

  }

}
