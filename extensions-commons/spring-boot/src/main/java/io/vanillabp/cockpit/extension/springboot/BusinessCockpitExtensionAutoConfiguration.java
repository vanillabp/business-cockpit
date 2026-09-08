package io.vanillabp.cockpit.extension.springboot;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ResolvableType;

import io.vanillabp.cockpit.extension.BusinessCockpitAssembly;
import io.vanillabp.cockpit.extension.BusinessCockpitExtension;
import io.vanillabp.cockpit.extension.config.BusinessCockpitConfiguration;
import io.vanillabp.cockpit.extension.config.CockpitSettings;
import io.vanillabp.cockpit.extension.outbox.BusinessCockpitOutbox;
import io.vanillabp.cockpit.extension.service.BusinessCockpitServiceFactory;
import io.vanillabp.cockpit.extension.spi.BusinessCockpitBpmsBridge;
import io.vanillabp.cockpit.extension.templating.Templating;
import io.vanillabp.cockpit.extension.wiring.BusinessCockpitWiringService;
import io.vanillabp.integration.adapter.migration.config.MigrationAdapterProperties;
import io.vanillabp.integration.adapter.migration.processservice.PhaseTwoOutboxResolver;
import io.vanillabp.integration.adapter.migration.processservice.TransactionRunnerResolver;
import io.vanillabp.integration.extension.spi.ExtensionWiringService;
import io.vanillabp.integration.extension.spi.handler.ExtensionHandlers;
import io.vanillabp.integration.extension.spi.service.AggregateServiceFactory;
import io.vanillabp.integration.spi.PhaseOperationRegistry;
import io.vanillabp.integration.spi.PhaseTwoOutbox;
import io.vanillabp.integration.spi.PhaseTwoOutboxAware;
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
@EnableConfigurationProperties(CockpitOverlayProperties.class)
public class BusinessCockpitExtensionAutoConfiguration implements DisposableBean {

  private BusinessCockpitExtension extension;

  /**
   * What the application wrote about the Business Cockpit, read off this platform's binding and
   * handed on as the neutral object the core and every BPMS half read.
   *
   * @param overlay The cockpit's overlay of the shared <code>vanillabp.*</code> tree
   * @return The settings
   */
  @Bean
  public CockpitSettings businessCockpitSettings(
      final CockpitOverlayProperties overlay) {

    return overlay.toSettings();

  }

  /**
   * The extension itself, built from what the application configured.
   * <p>
   * It is also the {@link io.vanillabp.cockpit.extension.spi.BusinessCockpitEventPublisher} a
   * BPMS half injects: one bean, so that there is nothing to tell apart.
   *
   * @param properties VanillaBP's resolved configuration, which knows the application's
   *          workflow modules and adapters
   * @param settings What the application wrote below the cockpit's own sections
   * @param bridges The BPMS halves the application brought
   * @param workflowModuleDetailsProviders What the application says about its modules
   * @param handlers VanillaBP's invocation of the details providers
   * @param outboxResolvers VanillaBP's attribution of an outbox store to a workflow aggregate
   * @param outboxes The outbox stores of the application
   * @param outboxAwares The stores an application named for single workflow aggregates
   * @param transactionRunners VanillaBP's attribution of a transaction to a workflow aggregate
   * @param applicationContext Where a bean holding a list of BPMS halves is looked up
   * @return The extension
   */
  @Bean
  public BusinessCockpitExtension businessCockpitExtension(
      final MigrationAdapterProperties properties,
      final CockpitSettings settings,
      final ObjectProvider<BusinessCockpitBpmsBridge> bridges,
      final ObjectProvider<WorkflowModuleDetailsProvider> workflowModuleDetailsProviders,
      final ExtensionHandlers handlers,
      final ObjectProvider<PhaseTwoOutboxResolver> outboxResolvers,
      final ObjectProvider<PhaseTwoOutbox> outboxes,
      final ObjectProvider<PhaseTwoOutboxAware<?>> outboxAwares,
      final TransactionRunnerResolver transactionRunners,
      final ApplicationContext applicationContext) {

    final var configuration = BusinessCockpitConfiguration
        .readAndValidate(properties, settings, Templating.engineAvailable());
    extension = new BusinessCockpitExtension(
        configuration, BusinessCockpitAssembly.transportOf(configuration), theBridges(
            bridges,
            applicationContext), workflowModuleDetailsProviders.stream().toList(), handlers, BusinessCockpitAssembly
                .templatingOf(configuration), theOutbox(
                    outboxResolvers, outboxes, outboxAwares), transactionRunners);
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
   * Resolves the outbox store and the transaction of every workflow aggregate once the
   * application's singletons exist, which is what makes a store nobody can attribute end the
   * boot rather than the first report.
   * <p>
   * It waits like VanillaBP's own startup validation does: asking for the store of an aggregate
   * reaches into the application's persistence, and doing that while beans are still being
   * created would materialize repositories half way through the boot. By then VanillaBP has
   * registered the workflow services of every process service too, which is what the extension
   * asks for the aggregate of a BPMN process.
   *
   * @param extension The extension
   * @return The startup validation
   */
  @Bean
  public SmartInitializingSingleton businessCockpitOutboxStartupValidation(
      final BusinessCockpitExtension extension) {

    return extension::validateWhereEntriesAreWritten;

  }

  /**
   * @param extension The extension
   * @return The factory building the <code>BusinessCockpitService</code> of each workflow
   *         aggregate. The declared generic names the service interface, which is how
   *         VanillaBP learns what to inject
   */
  @Bean
  public AggregateServiceFactory<BusinessCockpitService> businessCockpitServiceFactory(
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
   * Every BPMS half of this application, however it was produced.
   * <p>
   * A BPMS half serving several configured adapter ids of its BPMS cannot always say at build
   * time how many bridges that is, so it may produce them as one list bean instead of one bean
   * each - the shape its Quarkus half has to use and which is therefore accepted here as well.
   *
   * @param bridges The bridges produced one by one
   * @param applicationContext Where a bean holding a list of them is looked up
   * @return All of them
   */
  private static Collection<BusinessCockpitBpmsBridge> theBridges(
      final ObjectProvider<BusinessCockpitBpmsBridge> bridges,
      final ApplicationContext applicationContext) {

    final var found = new LinkedHashSet<BusinessCockpitBpmsBridge>(bridges.stream().toList());
    for (final var beanName : applicationContext
        .getBeanNamesForType(
            ResolvableType.forClassWithGenerics(List.class, BusinessCockpitBpmsBridge.class))) {
      ((List<?>) applicationContext.getBean(beanName))
          .stream()
          .filter(Objects::nonNull)
          .map(BusinessCockpitBpmsBridge.class::cast)
          .forEach(found::add);
    }
    return found;

  }

  /**
   * Where the extension writes its entries - see decision 12 in the repository's DECISIONS.md.
   * <p>
   * The attribution of a store to a workflow aggregate is VanillaBP's own, so that an entry of
   * the extension lands where an entry of the core lands: in the store the aggregate's
   * transaction reaches. What to do about a missing store is the resolver's answer too - the
   * remedies depend on what this platform can provide, and repeating them here would be a
   * second list to keep in step.
   */
  private static BusinessCockpitOutbox theOutbox(
      final ObjectProvider<PhaseTwoOutboxResolver> outboxResolvers,
      final ObjectProvider<PhaseTwoOutbox> outboxes,
      final ObjectProvider<PhaseTwoOutboxAware<?>> outboxAwares) {

    final var resolver = outboxResolvers.getIfAvailable();
    if (resolver == null) {
      throw new IllegalStateException(
          """
              The Business Cockpit extension found no bean of type %s, which VanillaBP's Spring \
              Boot integration provides and which says which outbox store holds the entries of a \
              workflow aggregate. Check that the application runs a version of \
              io.vanillabp:vanillabp-spring-boot-integration which matches the extension."""
              .formatted(PhaseTwoOutboxResolver.class.getName()));
    }
    return new BusinessCockpitOutbox(
        resolver::resolveFor, () -> storesOf(outboxes, outboxAwares), resolver.remediesDescription());

  }

  /**
   * Every store the application holds, the ones named for a single workflow aggregate included:
   * an application whose stores are all provided that way has no plain store bean at all, and
   * telling it to add a data source would be wrong.
   */
  private static Collection<PhaseTwoOutbox> storesOf(
      final ObjectProvider<PhaseTwoOutbox> outboxes,
      final ObjectProvider<PhaseTwoOutboxAware<?>> outboxAwares) {

    return Stream
        .concat(
            outboxes.stream(),
            outboxAwares.stream().map(PhaseTwoOutboxAware::getPhaseTwoOutbox))
        .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

  }

}
