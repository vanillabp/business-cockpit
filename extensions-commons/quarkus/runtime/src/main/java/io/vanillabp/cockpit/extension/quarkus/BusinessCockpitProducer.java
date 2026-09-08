package io.vanillabp.cockpit.extension.quarkus;

import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vanillabp.cockpit.extension.BusinessCockpitAssembly;
import io.vanillabp.cockpit.extension.BusinessCockpitExtension;
import io.vanillabp.cockpit.extension.config.BusinessCockpitConfiguration;
import io.vanillabp.cockpit.extension.service.BusinessCockpitServiceFactory;
import io.vanillabp.cockpit.extension.spi.BusinessCockpitBpmsBridge;
import io.vanillabp.cockpit.extension.templating.Templating;
import io.vanillabp.cockpit.extension.wiring.BusinessCockpitWiringService;
import io.vanillabp.integration.adapter.migration.config.MigrationAdapterProperties;
import io.vanillabp.integration.extension.spi.ExtensionWiringService;
import io.vanillabp.integration.extension.spi.handler.ExtensionHandlers;
import io.vanillabp.integration.extension.spi.service.AggregateServiceFactory;
import io.vanillabp.integration.spi.PhaseOperationRegistry;
import io.vanillabp.integration.spi.PhaseTwoOutbox;
import io.vanillabp.spi.cockpit.BusinessCockpitService;
import io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleDetailsProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.transaction.TransactionSynchronizationRegistry;

/**
 * Registers the Business Cockpit extension on Quarkus - the twin of the Spring Boot module's
 * auto-configuration, doing the same things with CDI.
 * <p>
 * The producers are <code>&#64;Singleton</code> rather than
 * <code>&#64;ApplicationScoped</code>: what they produce has no no-argument constructor and is
 * therefore not client-proxyable.
 */
@ApplicationScoped
public class BusinessCockpitProducer {

  /**
   * The extension itself, built from what the application configured.
   * <p>
   * It is also the {@link io.vanillabp.cockpit.extension.spi.BusinessCockpitEventPublisher} a
   * BPMS half injects: one bean, so that there is nothing to tell apart.
   *
   * @param properties VanillaBP's resolved configuration
   * @param bridges The BPMS halves the application brought
   * @param workflowModuleDetailsProviders What the application says about its modules
   * @param handlers VanillaBP's invocation of the details providers
   * @param outboxes The outbox stores of the application
   * @param transactionRegistry What tells whether a transaction is running
   * @return The extension
   */
  @Produces
  @Singleton
  @Unremovable
  public BusinessCockpitExtension businessCockpitExtension(
      final MigrationAdapterProperties properties,
      @Any final Instance<BusinessCockpitBpmsBridge> bridges,
      @Any final Instance<WorkflowModuleDetailsProvider> workflowModuleDetailsProviders,
      final ExtensionHandlers handlers,
      @Any final Instance<PhaseTwoOutbox> outboxes,
      final TransactionSynchronizationRegistry transactionRegistry) {

    final var configuration = BusinessCockpitConfiguration
        .readAndValidate(properties, Templating.engineAvailable());
    return new BusinessCockpitExtension(
        configuration, BusinessCockpitAssembly.transportOf(configuration), bridges.stream()
            .toList(), workflowModuleDetailsProviders.stream().toList(), handlers, BusinessCockpitAssembly
                .templatingOf(configuration), theOutbox(outboxes), new QuarkusTransactionRunner(transactionRegistry));

  }

  /**
   * @param extension The extension
   * @return The factory building the <code>BusinessCockpitService</code> of each workflow
   *         aggregate
   */
  @Produces
  @Singleton
  @Unremovable
  public AggregateServiceFactory<BusinessCockpitService> businessCockpitServiceFactory(
      final BusinessCockpitExtension extension) {

    return new BusinessCockpitServiceFactory(extension);

  }

  /**
   * @param extension The extension
   * @return Its place in VanillaBP's deployment pipeline
   */
  @Produces
  @Singleton
  @Unremovable
  public ExtensionWiringService<Object, Object> businessCockpitWiringService(
      final BusinessCockpitExtension extension) {

    return new BusinessCockpitWiringService(extension);

  }

  /**
   * Announces the extension to VanillaBP once the application is up: its three outbox
   * operations and the two contracts of its details providers.
   *
   * @param event The startup
   * @param extension The extension
   * @param registry VanillaBP's registry of phase-two operations
   */
  void register(
      @Observes final StartupEvent event,
      final BusinessCockpitExtension extension,
      final PhaseOperationRegistry registry) {

    extension.registerOperations(registry);
    extension.registerHandlerContracts();

  }

  /**
   * Registers the workflow modules at the cockpit server.
   * <p>
   * The priority is what makes this run late: VanillaBP's outbox store creates its table in a
   * startup observer of its own, after the deployment pipeline noted which workflow modules
   * started, so an entry written any earlier would find no table.
   *
   * @param event The startup
   * @param extension The extension
   */
  void registerWorkflowModules(
      @Observes
      @jakarta.annotation.Priority(
        jakarta.interceptor.Interceptor.Priority.APPLICATION + 800) final StartupEvent event,
      final BusinessCockpitExtension extension) {

    extension.registerStartedWorkflowModules();

  }

  /**
   * Releases what the transport holds.
   *
   * @param event The shutdown
   * @param extension The extension
   */
  void release(
      @Observes final ShutdownEvent event,
      final BusinessCockpitExtension extension) {

    extension.stop();

  }

  /**
   * The one store the extension writes into - see decision 8 in the repository's DECISIONS.md.
   */
  private static PhaseTwoOutbox theOutbox(
      final Instance<PhaseTwoOutbox> outboxes) {

    final var found = outboxes.stream().toList();
    if (found.size() == 1) {
      return found.getFirst();
    }
    throw new IllegalStateException(
        found.isEmpty()
            ? """
                The Business Cockpit extension needs an outbox store: it reports every event after \
                the transaction which caused it was committed, which is what keeps an event from \
                being reported for something that was rolled back. Add the extension \
                'quarkus-agroal' with a data source, or 'quarkus-mongodb-client', whereupon \
                VanillaBP provides the store."""
            : """
                The Business Cockpit extension found %d outbox stores in this application (%s) and \
                cannot tell which of them its events belong in. Leave one of them."""
                .formatted(
                    found.size(),
                    String
                        .join(
                            ", ",
                            found.stream().map(outbox -> outbox.getClass().getName()).toList())));

  }

}
