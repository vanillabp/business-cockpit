package io.vanillabp.cockpit.support.security.jwt;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.support.security.usercontext.DirectoryOfTheTest;
import io.vanillabp.cockpit.support.security.usercontext.FormResource;
import io.vanillabp.cockpit.support.security.usercontext.GroupResolver;
import io.vanillabp.cockpit.support.security.usercontext.UserDetailsImpl;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * The login token names the user, and the groups come from the user directory on every request. So
 * a group the directory takes away is gone on the next request, and a token of an older version,
 * which still names groups, keeps working without its groups being used. It is the twin of the
 * Spring Boot test of the same name.
 * <p>
 * The application of this test has a group hierarchy of its own: {@code ADMIN} implies
 * {@code TASK_VIEWER}.
 */
@ExtendWith(SuppressOutputExtension.class)
public class GroupsComeFromTheDirectoryTest {

  /**
   * The group hierarchy of the test application. It replaces the resolver of the module.
   */
  @ApplicationScoped
  public static class HierarchyOfTheTest implements GroupResolver {

    @Override
    public Collection<String> resolve(
        final Collection<String> groups) {

      final var resolved = new HashSet<>(groups);
      if (groups.contains("ADMIN")) {
        resolved.add("TASK_VIEWER");
      }
      return resolved;

    }

  }

  @RegisterExtension
  static final QuarkusExtensionTest application = new QuarkusExtensionTest()
      .withApplicationRoot(jar -> TheSupportOnQuarkus
          .withTheSupport(jar)
          .addClasses(FormResource.class, DirectoryOfTheTest.class, HierarchyOfTheTest.class))
      .overrideConfigKey(JwtSettings.SECRET_KEY, TheSupportOnQuarkus.SECRET_BASE64)
      .setLogRecordPredicate(record -> record.getLoggerName().equals(LoginTokenIdentityProvider.class.getName()))
      .assertLogRecords(records -> {
        assertThat(records)
            .filteredOn(record -> record.getLevel().intValue() == Level.WARNING.intValue())
            .filteredOn(record -> textOf(record).contains("ghost"))
            .as("three requests of an unknown user within a minute give one warning")
            .singleElement()
            .satisfies(record -> assertThat(textOf(record)).contains("USER_"));
        assertThat(records)
            .filteredOn(record -> record.getLevel().intValue() == Level.SEVERE.intValue())
            .singleElement()
            .satisfies(record -> assertThat(record.getThrown()).hasMessage("LDAP is down"));
      });

  @Inject
  DirectoryOfTheTest directory;

  @BeforeEach
  public void setUp() {

    directory.clear();
    directory.with("john", "ADMIN", "SALES");

  }

  @Test
  public void aGroupTakenAwayInTheDirectoryIsGoneOnTheNextRequest() {

    final var token = TheSupportOnQuarkus.loginOf("john");
    assertRoles(token, "ADMIN,SALES,TASK_VIEWER,USER_john");

    directory.with("john", "SALES");

    assertRoles(token, "SALES,USER_john");

  }

  @Test
  public void anOldTokenWithGroupsIsAcceptedAndItsGroupsAreNotUsed() {

    final var oldToken = LoginTokens.signed(TheSupportOnQuarkus.secret(), "john", claims -> claims
        .claim("authorities", List.of("ADMIN", "FORMER_TEAM")));
    directory.with("john", "SALES");

    assertRoles(oldToken, "SALES,USER_john");

    directory.with("john");

    assertRoles(oldToken, "USER_john");

  }

  @Test
  public void theGroupHierarchyResolvesTheGroupsOfTheDirectory() {

    assertRoles(TheSupportOnQuarkus.loginOf("john"), "ADMIN,SALES,TASK_VIEWER,USER_john");

  }

  @Test
  public void aUserTheDirectoryDoesNotKnowHasOnlyItsOwnAuthorityAndIsWarnedAboutOnce() {

    final var token = TheSupportOnQuarkus.loginOf("ghost");

    assertRoles(token, "USER_ghost");
    assertRoles(token, "USER_ghost");
    given()
        .cookie("bc", token)
        .get("/form/free")
        .then()
        .statusCode(200)
        .body(equalTo("ghost"));

  }

  @Test
  public void aFailingDirectoryFailsTheRequest() {

    directory.failWith(new IllegalStateException("LDAP is down"));

    given()
        .cookie("bc", TheSupportOnQuarkus.loginOf("john"))
        .get("/form/free")
        .then()
        .statusCode(500);

  }

  @Test
  public void theNameAndTheMailComeFromTheDirectoryAndOtherwiseFromTheToken() {

    directory.with(new UserDetailsImpl("john", "john@example.com", "Doe, John", "Doe, J.", List.of("ADMIN")));
    final var token = LoginTokens.signed(TheSupportOnQuarkus.secret(), "john", claims -> claims
        .claim("name", "Name Token")
        .claim("email", "token@example.com"));

    given()
        .cookie("bc", token)
        .get("/form/details")
        .then()
        .statusCode(200)
        .body(equalTo("john\nDoe, John\nDoe, J.\njohn@example.com\nADMIN,TASK_VIEWER,USER_john"));

    directory.remove("john");

    given()
        .cookie("bc", token)
        .get("/form/details")
        .then()
        .statusCode(200)
        .body(equalTo("john\nName Token\nName, Token\ntoken@example.com\nUSER_john"));

  }

  /**
   * The message of a log record together with its parameters, whichever of the two holds the user
   * id.
   */
  private static String textOf(
      final LogRecord record) {

    return record.getMessage()
        + " "
        + Arrays.toString(record.getParameters());

  }

  private static void assertRoles(
      final String token,
      final String roles) {

    given()
        .cookie("bc", token)
        .get("/form/roles")
        .then()
        .statusCode(200)
        .body(equalTo(roles));

  }

}
