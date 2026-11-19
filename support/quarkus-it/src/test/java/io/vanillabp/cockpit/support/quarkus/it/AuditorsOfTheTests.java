package io.vanillabp.cockpit.support.quarkus.it;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * An augmentor of the application: carol is an auditor, whatever her login says. It shows that
 * {@code TestLogin} runs the augmentors of the application, and not only those Quarkus brings.
 */
@ApplicationScoped
public class AuditorsOfTheTests implements SecurityIdentityAugmentor {

  /** The role this augmentor adds. */
  public static final String AUDITOR = "AUDITOR";

  @Override
  public Uni<SecurityIdentity> augment(
      final SecurityIdentity identity,
      final AuthenticationRequestContext context) {

    if (identity.isAnonymous() || !"carol".equals(identity.getPrincipal().getName())) {
      return Uni.createFrom().item(identity);
    }
    return Uni
        .createFrom()
        .item(QuarkusSecurityIdentity
            .builder(identity)
            .addRole(AUDITOR)
            .build());

  }

}
