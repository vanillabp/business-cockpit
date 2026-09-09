package io.vanillabp.cockpit.extension.springboot;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import io.vanillabp.cockpit.extension.BusinessCockpitExtension;
import io.vanillabp.integration.spi.PhaseOperationRegistry;

/**
 * Announces the extension to VanillaBP: the three outbox operations it dispatches itself, and
 * the two contracts telling VanillaBP how to find and invoke the application's details
 * providers.
 * <p>
 * Both happen once, and neither cares whether the application's workflow services have been
 * scanned yet - a contract registered late is applied to what was scanned before.
 */
public class BusinessCockpitRegistrar implements InitializingBean, ApplicationListener<ApplicationReadyEvent> {

  private final BusinessCockpitExtension extension;

  private final PhaseOperationRegistry registry;

  /**
   * @param extension The extension
   * @param registry VanillaBP's registry of phase-two operations
   */
  public BusinessCockpitRegistrar(
      final BusinessCockpitExtension extension,
      final PhaseOperationRegistry registry) {

    this.extension = extension;
    this.registry = registry;

  }

  @Override
  public void afterPropertiesSet() {

    extension.registerOperations(registry);
    extension.registerHandlerContracts();

  }

  /**
   * Registers the workflow modules at the cockpit server once the application is up. Doing it
   * here rather than while the modules are deployed is what the Quarkus half needs, and doing
   * it the same way on both platforms is what keeps the two halves comparable.
   *
   * @param event The application having started
   */
  @Override
  public void onApplicationEvent(
      final ApplicationReadyEvent event) {

    extension.registerStartedWorkflowModules();

  }

}
