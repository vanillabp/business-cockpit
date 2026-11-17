package io.vanillabp.cockpit.gui.api.v1.client.quarkus;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.quarkus.runtime.Startup;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialCurrentUserApi;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialTasklistApi;
import io.vanillabp.cockpit.support.rest.restclient.RestClients;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;

/**
 * What the workflow module 'loan-approval' writes to call the cockpit: its clients, built from the
 * keys at its own prefix while the application starts, and a qualifier which tells them apart from
 * the clients of other modules.
 */
@ApplicationScoped
public class LoanApprovalCockpit {

  /** The clients of the workflow module 'loan-approval'. */
  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  public @interface LoanApproval {
  }

  @Produces
  @Singleton
  @Startup
  @LoanApproval
  OfficialTasklistApi tasklist(
      final RestClients restClients) {
    return restClients.build(OfficialTasklistApi.class, "loan-approval.cockpit");
  }

  @Produces
  @Singleton
  @Startup
  @LoanApproval
  OfficialCurrentUserApi currentUser(
      final RestClients restClients) {
    return restClients.build(OfficialCurrentUserApi.class, "loan-approval.cockpit");
  }

}
