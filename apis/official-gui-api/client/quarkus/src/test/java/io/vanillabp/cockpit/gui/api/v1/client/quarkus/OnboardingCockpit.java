package io.vanillabp.cockpit.gui.api.v1.client.quarkus;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.quarkus.runtime.Startup;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialTasklistApi;
import io.vanillabp.cockpit.support.rest.restclient.RestClients;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;

/**
 * What a second workflow module, 'onboarding', writes: the same generated interface, built from
 * the keys at its own prefix, with another address and another read timeout.
 */
@ApplicationScoped
public class OnboardingCockpit {

  /** The clients of the workflow module 'onboarding'. */
  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Onboarding {
  }

  @Produces
  @Singleton
  @Startup
  @Onboarding
  OfficialTasklistApi tasklist(
      final RestClients restClients) {
    return restClients.build(OfficialTasklistApi.class, "onboarding.cockpit");
  }

}
