package io.vanillabp.cockpit.support.rest.restclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.Startup;
import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.support.rest.restclient.inventory.InventoryApi;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ProcessingException;
import mockwebserver3.MockWebServer;

/**
 * Two workflow modules of one application call the same service through the same generated
 * interface, each with its own keys: another address and another read timeout. Each module builds
 * its client at a prefix of its own and tells its bean apart by a qualifier.
 */
@ExtendWith(SuppressOutputExtension.class)
public class TwoModulesTest {

  private static final String STUB = "two-modules";

  @RegisterExtension
  static final QuarkusExtensionTest application = new QuarkusExtensionTest()
      .withApplicationRoot(jar -> TheRestClientsOnQuarkus
          .withTheRestClients(jar)
          .addClasses(LoanApproval.class, Onboarding.class, LoanApprovalClients.class, OnboardingClients.class))
      .overrideConfigKey("loan-approval.inventory.base-url", Stubs.url(STUB, "/loans"))
      .overrideConfigKey("loan-approval.inventory.read-timeout", "300ms")
      .overrideConfigKey("onboarding.inventory.base-url", Stubs.url(STUB, "/onboarding"))
      .overrideConfigKey("onboarding.inventory.read-timeout", "5s");

  /** The clients of the workflow module 'loan-approval'. */
  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  public @interface LoanApproval {
  }

  /** The clients of the workflow module 'onboarding'. */
  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Onboarding {
  }

  /** What the module 'loan-approval' writes. */
  @ApplicationScoped
  public static class LoanApprovalClients {

    @Produces
    @Singleton
    @Startup
    @LoanApproval
    InventoryApi inventory(
        final RestClients restClients) {
      return restClients.build(InventoryApi.class, "loan-approval.inventory");
    }

  }

  /** What the module 'onboarding' writes. */
  @ApplicationScoped
  public static class OnboardingClients {

    @Produces
    @Singleton
    @Startup
    @Onboarding
    InventoryApi inventory(
        final RestClients restClients) {
      return restClients.build(InventoryApi.class, "onboarding.inventory");
    }

  }

  static final MockWebServer server = new MockWebServer();

  @AfterAll
  static void stop() {
    server.close();
  }

  @Inject
  @LoanApproval
  InventoryApi loanApproval;

  @Inject
  @Onboarding
  InventoryApi onboarding;

  @BeforeEach
  void startTheStub() {

    Stubs.started(server, STUB);
    Stubs.drain(server);

  }

  @Test
  public void each_module_calls_its_own_address() {

    server.enqueue(Stubs.item("1"));
    server.enqueue(Stubs.item("2"));

    loanApproval.getItem("1");
    onboarding.getItem("2");

    assertThat(Stubs.next(server).getTarget()).isEqualTo("/loans/items/1");
    assertThat(Stubs.next(server).getTarget()).isEqualTo("/onboarding/items/2");

  }

  @Test
  public void each_module_waits_as_long_as_its_own_keys_say() {

    server.enqueue(Stubs.tooSlow());
    assertThatThrownBy(() -> loanApproval.getItem("1")).isInstanceOf(ProcessingException.class);

    server.enqueue(Stubs.tooSlow());
    onboarding.getItem("2");

  }

}
