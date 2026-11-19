package io.vanillabp.cockpit.support.quarkus.it;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.SecurityIdentity;
import io.vanillabp.cockpit.support.security.usercontext.UserContext;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * A service of a workflow module whose methods its tests call as beans. A permission checker
 * decides who may assess a risk, the way a module asks its aggregate whether the user may work on a
 * task. Quarkus brings the checker to the identity of a login through a
 * {@code SecurityIdentityAugmentor}.
 */
@ApplicationScoped
public class RiskAssessment {

  @Inject
  UserContext userContext;

  @PermissionsAllowed("assess-risk")
  public String assess() {

    return "assessed by "
        + userContext.getUserLoggedIn();

  }

  @RolesAllowed(AuditorsOfTheTests.AUDITOR)
  public String audit() {

    return "audited by "
        + userContext.getUserLoggedIn();

  }

  @PermissionChecker("assess-risk")
  boolean mayAssessARisk(
      final SecurityIdentity identity) {

    return identity.hasRole("CLERK");

  }

}
