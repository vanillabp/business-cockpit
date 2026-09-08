package io.vanillabp.cockpit.extension.springboot;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

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
import io.vanillabp.integration.spi.PhaseOperationRegistry;
import io.vanillabp.integration.spi.PhaseTwoOutbox;
import io.vanillabp.spi.cockpit.BusinessCockpitService;
import io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleDetailsProvider;

/**
 * Registers the Business Cockpit extension on Spring Boot.
 * <p>
 * Nothing here decides anything. The configuration is read and validated by the neutral core,
 * the transport is chosen there too, and this class does what only Spring can do: find the
 * beans, hand them over, and put the extension's own beans where VanillaBP collects them.
 * <p>
 * It runs after VanillaBP's own auto-configuration, named rather than referenced, because an
 * extension does not compile against a platform integration.
 */
@AutoConfiguration(afterName = "io.vanillabp.integration.processservice.SpringBootMigrationAdapterAutoConfiguration")
@ConditionalOnBean({
    MigrationAdapterProperties.class, ExtensionHandlers.class
})
public class BusinessCockpitExtensionAutoConfiguration implements DisposableBean {

  private BusinessCockpitExtension extension;

  /**
   * The extension itself, built from what the application configured.
   * <p>
   * It is also the {@link io.vanillabp.cockpit.extension.spi.BusinessCockpitEventPublisher} a
   * BPMS half injects: one bean, so that there is nothing to tell apart.
   *
   * @param properties VanillaBP's resolved configuration, which owns the two locations an
   *          extension is configured in
   * @param bridges The BPMS halves the application brought
   * @param workflowModuleDetailsProviders What the application says about its modules
   * @param handlers VanillaBP's invocation of the details providers
   * @param outboxes The outbox stores of the application
   * @param transactionManagers The transaction managers of the application
   * @return The extension
   */
  @Bean
  public BusinessCockpitExtension businessCockpitExtension(
      final MigrationAdapterProperties properties,
      final ObjectProvider<BusinessCockpitBpmsBridge> bridges,
      final ObjectProvider<WorkflowModuleDetailsProvider> workflowModuleDetailsProviders,
      final ExtensionHandlers handlers,
      final ObjectProvider<PhaseTwoOutbox> outboxes,
      final ObjectProvider<PlatformTransactionManager> transactionManagers) {

    final var configuration = BusinessCockpitConfiguration
        .readAndValidate(properties, Templating.engineAvailable());
    extension = new BusinessCockpitExtension(
        configuration, BusinessCockpitAssembly.transportOf(configuration), bridges.stream()
            .toList(), workflowModuleDetailsProviders.stream().toList(), handlers, BusinessCockpitAssembly
                .templatingOf(configuration), theOutbox(
                    outboxes), new SpringTransactionRunner(theTransactionManager(transactionManagers)));
    return extension;

  }

  /**
   * Registers the extension's outbox operations and the contracts of its details providers.
   * Both may happen at any point of the startup, so a bean of its own is enough.
   *
   * @param extension The extension
   * @param registry VanillaBP's registry of phase-two operations
   * @return The registrar
   */
  @Bean
  public BusinessCockpitRegistrar businessCockpitRegistrar(
      final BusinessCockpitExtension extension,
      final PhaseOperationRegistry registry) {

    return new BusinessCockpitRegistrar(extension, registry);

  }

  /**
   * @param extension The extension
   * @return The factory building the <code>BusinessCockpitService</code> of each workflow
   *         aggregate. The declared generic names the service interface, which is how
   *         VanillaBP learns what to inject
   */
  @Bean
  public io.vanillabp.integration.extension.spi.service.AggregateServiceFactory<BusinessCockpitService> businessCockpitServiceFactory(
      final BusinessCockpitExtension extension) {

    return new BusinessCockpitServiceFactory(extension);

  }

  /**
   * @param extension The extension
   * @return Its place in VanillaBP's deployment pipeline
   */
  @Bean
  public ExtensionWiringService<Object, Object> businessCockpitWiringService(
      final BusinessCockpitExtension extension) {

    return new BusinessCockpitWiringService(extension);

  }

  @Override
  public void destroy() {

    if (extension != null) {
      extension.stop();
    }

  }

  /**
   * The one store the extension writes into - see decision 8 in the repository's DECISIONS.md.
   */
  private static PhaseTwoOutbox theOutbox(
      final ObjectProvider<PhaseTwoOutbox> outboxes) {

    final var unique = outboxes.getIfUnique();
    if (unique != null) {
      return unique;
    }
    final var found = outboxes.stream().map(outbox -> outbox.getClass().getName()).toList();
    throw new IllegalStateException(
        found.isEmpty()
            ? """
                The Business Cockpit extension needs an outbox store: it reports every event after \
                the transaction which caused it was committed, which is what keeps an event from \
                being reported for something that was rolled back. Add a relational data source \
                (spring-boot-starter-data-jpa) or a MongoDB connection to your workflow module, \
                whereupon VanillaBP provides the store."""
            : """
                The Business Cockpit extension found %d outbox stores in this application (%s) \
                and cannot tell which of them its events belong in. Leave one of them, or give the \
                extension one by defining a single bean of type \
                io.vanillabp.integration.spi.PhaseTwoOutbox marked as primary."""
                .formatted(found.size(), String.join(", ", found)));

  }

  private static PlatformTransactionManager theTransactionManager(
      final ObjectProvider<PlatformTransactionManager> transactionManagers) {

    final var unique = transactionManagers.getIfUnique();
    if (unique != null) {
      return unique;
    }
    final var found = transactionManagers
        .stream()
        .map(manager -> manager.getClass().getName())
        .toList();
    throw new IllegalStateException(
        found.isEmpty()
            ? """
                The Business Cockpit extension needs a transaction manager: the outbox entry of an \
                event reported by a remote engine is written on a worker thread which brings no \
                transaction of its own. Add the persistence your workflow aggregates live in \
                (spring-boot-starter-data-jpa, or a MongoTransactionManager for MongoDB)."""
            : """
                The Business Cockpit extension found %d transaction managers in this application \
                (%s) and cannot tell which one covers the workflow aggregates. Mark the one to use \
                as primary."""
                .formatted(found.size(), String.join(", ", found)));

  }

}
