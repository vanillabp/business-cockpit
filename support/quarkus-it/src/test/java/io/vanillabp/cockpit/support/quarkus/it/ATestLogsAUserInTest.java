package io.vanillabp.cockpit.support.quarkus.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestIdentityAssociation;
import io.vanillabp.cockpit.support.test.TestLogin;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.inject.Inject;

/**
 * A test of a workflow module calls the beans of the module as different users, one after the
 * other. {@link TestLogin} logs each one in, and the permission checker of the module decides as it
 * does for a real request. The test needs no loop over the augmentors of its own.
 */
@QuarkusTest
@ExtendWith(SuppressOutputExtension.class)
public class ATestLogsAUserInTest {

  @Inject
  TestLogin login;

  @Inject
  RiskAssessment riskAssessment;

  @Inject
  TestIdentityAssociation identities;

  @AfterEach
  void logOut() {

    login.logOut();

  }

  @Test
  public void theUserChangesWithinOneTest() {

    login.logIn("john", "CLERK");
    assertThat(riskAssessment.assess()).isEqualTo("assessed by john");

    login.logIn("mallory");
    assertThatThrownBy(riskAssessment::assess).isInstanceOf(ForbiddenException.class);

    login.logIn("jane", "SALES", "CLERK");
    assertThat(riskAssessment.assess()).isEqualTo("assessed by jane");

  }

  /**
   * Why {@link TestLogin} exists: an identity set as it is, without the augmentors, does not reach
   * the permission checker, and the call is refused although the user has the role. If this test
   * fails one day, Quarkus runs the augmentors itself, and the loop of {@link TestLogin} can go.
   */
  @Test
  public void anIdentityWithoutTheAugmentorsIsRefused() {

    identities.setTestIdentity(QuarkusSecurityIdentity
        .builder()
        .setPrincipal(new QuarkusPrincipal("john"))
        .addRole("CLERK")
        .build());

    assertThatThrownBy(riskAssessment::assess).isInstanceOf(ForbiddenException.class);

  }

  @Test
  public void withoutAUserNothingIsAllowed() {

    login.logIn("john", "CLERK");
    login.logOut();

    assertThatThrownBy(riskAssessment::assess).isInstanceOf(UnauthorizedException.class);

  }

  @Test
  public void theAugmentorsOfTheApplicationRunAsWell() {

    final var carol = login.logIn("carol");

    assertThat(carol.getRoles()).containsExactly(AuditorsOfTheTests.AUDITOR);
    assertThat(riskAssessment.audit()).isEqualTo("audited by carol");
    login.logIn("john", "CLERK");
    assertThatThrownBy(riskAssessment::audit).isInstanceOf(ForbiddenException.class);

  }

}
