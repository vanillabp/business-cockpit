package io.vanillabp.cockpit.extension.springboot;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ResolvableType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;

import io.vanillabp.cockpit.extension.BusinessCockpitAssembly;
import io.vanillabp.cockpit.extension.BusinessCockpitExtension;
import io.vanillabp.cockpit.extension.config.BusinessCockpitConfiguration;
import io.vanillabp.cockpit.extension.outbox.BusinessCockpitOutbox;
import io.vanillabp.cockpit.extension.service.BusinessCockpitServiceFactory;
import io.vanillabp.cockpit.extension.spi.BusinessCockpitBpmsBridge;
import io.vanillabp.cockpit.extension.templating.Templating;
import io.vanillabp.cockpit.extension.wiring.BusinessCockpitWiringService;
import io.vanillabp.integration.adapter.migration.config.MigrationAdapterProperties;
import io.vanillabp.integration.adapter.migration.processservice.PhaseTwoOutboxResolver;
import io.vanillabp.integration.extension.spi.ExtensionWiringService;
import io.vanillabp.integration.extension.spi.handler.ExtensionHandlers;
import io.vanillabp.integration.extension.spi.service.AggregateServiceFactory;
import io.vanillabp.integration.spi.PhaseOperationRegistry;
import io.vanillabp.integration.spi.PhaseTwoOutbox;
import io.vanillabp.integration.spi.PhaseTwoOutboxAware;
import io.vanillabp.spi.cockpit.BusinessCockpitService;
import io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleDetailsProvider;
import io.vanillabp.spi.service.WorkflowService;

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
   * @param outboxResolvers VanillaBP's attribution of an outbox store to a workflow aggregate
   * @param outboxes The outbox stores of the application
   * @param outboxAwares The stores an application named for single workflow aggregates
   * @param transactionManagers The transaction managers of the application
   * @param applicationContext Where the application's workflow services are looked up, to check
   *          what they wrote into the annotations of the extension
   * @return The extension
   */
  @Bean
  public BusinessCockpitExtension businessCockpitExtension(
      final MigrationAdapterProperties properties,
      final ObjectProvider<BusinessCockpitBpmsBridge> bridges,
      final ObjectProvider<WorkflowModuleDetailsProvider> workflowModuleDetailsProviders,
      final ExtensionHandlers handlers,
      final ObjectProvider<PhaseTwoOutboxResolver> outboxResolvers,
      final ObjectProvider<PhaseTwoOutbox> outboxes,
      final ObjectProvider<PhaseTwoOutboxAware<?>> outboxAwares,
      final ObjectProvider<PlatformTransactionManager> transactionManagers,
      final ApplicationContext applicationContext) {

    final var configuration = BusinessCockpitConfiguration
        .readAndValidate(properties, Templating.engineAvailable());
    extension = new BusinessCockpitExtension(
        configuration, BusinessCockpitAssembly.transportOf(configuration), theBridges(
            bridges,
            applicationContext), workflowModuleDetailsProviders.stream().toList(), handlers, BusinessCockpitAssembly
                .templatingOf(configuration), theOutbox(
                    outboxResolvers, outboxes, outboxAwares), new SpringTransactionRunner(
                        theTransactionManager(transactionManagers)));
    extension.validateDetailsProviders(workflowServiceClasses(applicationContext));
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
   * Where the extension writes its entries - see decision 10 in the repository's DECISIONS.md.
   * <p>
   * The attribution of a store to a workflow aggregate is VanillaBP's own, so that an entry of
   * the extension lands where an entry of the core lands: in the store the aggregate's
   * transaction reaches.
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
        resolver::resolveFor, () -> storesOf(outboxes, outboxAwares), """
            - add spring-boot-starter-data-jpa and configure a data source, whereupon VanillaBP \
            provides the store,
            - add spring-boot-starter-data-mongodb and configure the MongoDB connection, or
            - define a bean implementing io.vanillabp.integration.spi.PhaseTwoOutbox storing \
            entries wherever your workflow aggregates live.""");

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

  /**
   * The classes of the application which may carry the extension's annotations.
   *
   * @param applicationContext Where the beans are registered
   * @return The workflow services, each as the class the application wrote rather than as the
   *         proxy Spring may have wrapped it in - a proxy carries no method annotations
   */
  private static Collection<Class<?>> workflowServiceClasses(
      final ApplicationContext applicationContext) {

    final Collection<Class<?>> classes = new LinkedHashSet<>();
    for (final var beanName : applicationContext
        .getBeanNamesForAnnotation(WorkflowService.class)) {
      final var type = applicationContext.getType(beanName);
      if (type != null) {
        classes.add(ClassUtils.getUserClass(type));
      }
    }
    return classes;

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
