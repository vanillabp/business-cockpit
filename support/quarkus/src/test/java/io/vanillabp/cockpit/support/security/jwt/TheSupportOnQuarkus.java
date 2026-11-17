package io.vanillabp.cockpit.support.security.jwt;

import java.util.Base64;

import org.jboss.shrinkwrap.api.spec.JavaArchive;

import io.vanillabp.cockpit.support.security.BcUnauthorizedExceptionMapper;
import io.vanillabp.cockpit.support.security.usercontext.GroupResolver;
import io.vanillabp.cockpit.support.security.usercontext.GroupsAsTheDirectoryGivesThem;
import io.vanillabp.cockpit.support.security.usercontext.UserContext;
import io.vanillabp.cockpit.support.usertask.BcUserTaskExceptionMapper;

/**
 * What the tests of this module put into the application they start. The test framework builds
 * that application only out of the classes a test names, so the main classes of this module are
 * named here once.
 */
public final class TheSupportOnQuarkus {

  /**
   * The secret of the tests, fixed and not random. The test framework loads a test class twice, once
   * for JUnit and once inside the application, and a random secret would differ between the two.
   */
  public static final String SECRET_BASE64 = "9kt7o2AC1AF/tV55Txl2MV5cnjtVr6azrL7I2ID0BUw=";

  private TheSupportOnQuarkus() {
  }

  /**
   * The secret of the tests as bytes.
   *
   * @return The secret
   */
  public static byte[] secret() {

    return Base64.getDecoder().decode(SECRET_BASE64);

  }

  /**
   * A login token for the user, signed the way the cockpit signs it.
   *
   * @param userId The user id
   * @return The token
   */
  public static String loginOf(
      final String userId) {

    return LoginTokens.signed(secret(), userId);

  }

  /**
   * Adds the main classes of this module to the application of a test.
   *
   * @param application The application of a test
   * @return The application
   */
  public static JavaArchive withTheSupport(
      final JavaArchive application) {

    return application
        .addClasses(
            JwtSettings.class,
            CockpitCookieAuthenticationMechanism.class,
            LoginTokenAuthenticationRequest.class,
            LoginTokenIdentityProvider.class,
            JwtStartupCheck.class,
            GroupResolver.class,
            GroupsAsTheDirectoryGivesThem.class,
            UserContext.class,
            BcUnauthorizedExceptionMapper.class,
            BcUserTaskExceptionMapper.class,
            TheSupportOnQuarkus.class);

  }

}
