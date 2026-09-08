package io.vanillabp.cockpit.extension.quarkus;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import io.quarkus.arc.Arc;
import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vanillabp.cockpit.extension.BusinessCockpitAssembly;
import io.vanillabp.cockpit.extension.BusinessCockpitExtension;
import io.vanillabp.cockpit.extension.config.BusinessCockpitConfiguration;
import io.vanillabp.cockpit.extension.outbox.BusinessCockpitOutbox;
import io.vanillabp.cockpit.extension.service.BusinessCockpitServiceFactory;
import io.vanillabp.cockpit.extension.spi.BusinessCockpitBpmsBridge;
import io.vanillabp.cockpit.extension.templating.Templating;
import io.vanillabp.cockpit.extension.wiring.BusinessCockpitWiringService;
import io.vanillabp.integration.adapter.migration.config.MigrationAdapterProperties;
import io.vanillabp.integration.adapter.migration.processservice.AwareSelection;
import io.vanillabp.integration.extension.spi.ExtensionWiringService;
import io.vanillabp.integration.extension.spi.handler.ExtensionHandlers;
import io.vanillabp.integration.extension.spi.service.AggregateServiceFactory;
import io.vanillabp.integration.spi.PhaseOperationRegistry;
import io.vanillabp.integration.spi.PhaseTwoOutbox;
import io.vanillabp.integration.spi.PhaseTwoOutboxAware;
import io.vanillabp.spi.cockpit.BusinessCockpitService;
import io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleDetailsProvider;
import io.vanillabp.spi.service.WorkflowService;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;
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
   * When the workflow modules are registered while the application starts.
   * <p>
   * It has to be later than the startup observer which creates the outbox store's table, which
   * VanillaBP's Quarkus integration runs at
   * <code>VanillaBpDeploymentRunner.OUTBOX_DISPATCHER_STARTUP_PRIORITY</code> - priority
   * <code>APPLICATION + 700</code> - because an entry written before that would find no table.
   * The number is written out rather than read from the integration: an extension does not
   * compile against a platform integration, so the two are kept in step by this comment.
   */
  static final int REGISTRATION_STARTUP_PRIORITY = Interceptor.Priority.APPLICATION + 800;

  /**
   * The extension itself, built from what the application configured.
   * <p>
   * It is also the {@link io.vanillabp.cockpit.extension.spi.BusinessCockpitEventPublisher} a
   * BPMS half injects: one bean, so that there is nothing to tell apart.
   *
   * @param properties VanillaBP's resolved configuration
   * @param bridges The BPMS halves the application brought
   * @param bridgeLists The BPMS halves an extension produced as one list, which is how a BPMS
   *          half builds a bridge per configured adapter id on Quarkus: how many there are is
   *          decided by the configuration and therefore not by a producer method
   * @param workflowModuleDetailsProviders What the application says about its modules
   * @param handlers VanillaBP's invocation of the details providers
   * @param outboxes The outbox stores of the application
   * @param outboxAwares The stores an application named for single workflow aggregates
   * @param transactionRegistry What tells whether a transaction is running
   * @return The extension
   */
  @Produces
  @Singleton
  @Unremovable
  public BusinessCockpitExtension businessCockpitExtension(
      final MigrationAdapterProperties properties,
      @Any final Instance<BusinessCockpitBpmsBridge> bridges,
      @Any final Instance<List<BusinessCockpitBpmsBridge>> bridgeLists,
      @Any final Instance<WorkflowModuleDetailsProvider> workflowModuleDetailsProviders,
      final ExtensionHandlers handlers,
      @Any final Instance<PhaseTwoOutbox> outboxes,
      @Any final Instance<PhaseTwoOutboxAware<?>> outboxAwares,
      final TransactionSynchronizationRegistry transactionRegistry) {

    final var configuration = BusinessCockpitConfiguration
        .readAndValidate(properties, Templating.engineAvailable());
    final var extension = new BusinessCockpitExtension(
        configuration, BusinessCockpitAssembly.transportOf(configuration), theBridges(
            bridges, bridgeLists), workflowModuleDetailsProviders.stream().toList(), handlers, BusinessCockpitAssembly
                .templatingOf(configuration), theOutbox(outboxes,
                    outboxAwares), new QuarkusTransactionRunner(transactionRegistry));
    extension.validateDetailsProviders(workflowServiceClasses());
    return extension;

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
      @Priority(REGISTRATION_STARTUP_PRIORITY) final StartupEvent event,
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
   * Every BPMS half of this application, however it was produced.
   * <p>
   * A BPMS half which serves several configured adapter ids of its BPMS cannot say at build
   * time how many bridges that is, so it produces them as one list - the same shape VanillaBP's
   * own Quarkus integration collects its adapter deployment services in.
   */
  private static List<BusinessCockpitBpmsBridge> theBridges(
      final Instance<BusinessCockpitBpmsBridge> bridges,
      final Instance<List<BusinessCockpitBpmsBridge>> bridgeLists) {

    return Stream
        .concat(
            bridges.stream(),
            bridgeLists.stream().filter(Objects::nonNull).flatMap(List::stream))
        .filter(Objects::nonNull)
        .toList();

  }

  /**
   * Where the extension writes its entries - see decision 10 in the repository's DECISIONS.md.
   * <p>
   * A store an application named for a workflow aggregate is used for that aggregate, the way
   * VanillaBP itself uses it. What VanillaBP does beyond that - attributing one of its two
   * default stores to the persistence which manages an aggregate - an extension cannot do,
   * because the classes saying which store serves which technology live in the platform
   * integration and an extension does not compile against it. An application running two stores
   * therefore says which one serves an aggregate, with a bean implementing
   * {@link PhaseTwoOutboxAware}, and is told so where it does not.
   */
  private static BusinessCockpitOutbox theOutbox(
      final Instance<PhaseTwoOutbox> outboxes,
      final Instance<PhaseTwoOutboxAware<?>> outboxAwares) {

    return new BusinessCockpitOutbox(
        workflowAggregateClass -> storeOf(outboxes, outboxAwares, workflowAggregateClass), () -> storesOf(outboxes,
            outboxAwares), """
                - add the 'quarkus-agroal' extension and configure a JDBC datasource, whereupon \
                VanillaBP provides the store,
                - add the 'quarkus-mongodb-client' extension and configure the MongoDB connection \
                including 'quarkus.mongodb.database', or
                - define a bean implementing io.vanillabp.integration.spi.PhaseTwoOutbox storing \
                entries wherever your workflow aggregates live.""");

  }

  /**
   * The store holding the entries of one workflow aggregate: the most specific
   * {@link PhaseTwoOutboxAware} bean covering its class, or the single store of the
   * application.
   *
   * @return The store, or <code>null</code> where the application has none
   */
  private static PhaseTwoOutbox storeOf(
      final Instance<PhaseTwoOutbox> outboxes,
      final Instance<PhaseTwoOutboxAware<?>> outboxAwares,
      final Class<?> workflowAggregateClass) {

    final var named = AwareSelection
        .mostSpecific(
            outboxAwares.stream().<PhaseTwoOutboxAware<?>>map(aware -> aware).toList(),
            PhaseTwoOutboxAware::getAggregateClass,
            workflowAggregateClass);
    if (named.isPresent()) {
      return named.get().getPhaseTwoOutbox();
    }
    final var found = storesOf(outboxes, outboxAwares);
    if (found.size() == 1) {
      return found.iterator().next();
    }
    if (found.isEmpty()) {
      return null;
    }
    throw new IllegalStateException(
        """
            The Business Cockpit extension cannot tell which of the outbox stores %s holds the \
            entries of the workflow aggregate '%s'. An entry has to be written in the very \
            transaction which persists that aggregate, so name the store: provide a bean \
            implementing io.vanillabp.integration.spi.PhaseTwoOutboxAware for this aggregate, \
            returning the store matching its persistence."""
            .formatted(
                found.stream().map(outbox -> outbox.getClass().getName()).toList(),
                workflowAggregateClass.getName()));

  }

  /**
   * Every store the application holds, the ones named for a single workflow aggregate included:
   * an application whose stores are all provided that way has no plain store bean at all, and
   * telling it to add a datasource would be wrong.
   */
  private static Collection<PhaseTwoOutbox> storesOf(
      final Instance<PhaseTwoOutbox> outboxes,
      final Instance<PhaseTwoOutboxAware<?>> outboxAwares) {

    return Stream
        .concat(
            outboxes.stream(),
            outboxAwares.stream().map(PhaseTwoOutboxAware::getPhaseTwoOutbox))
        .filter(Objects::nonNull)
        .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

  }

  /**
   * The classes of the application which may carry the extension's annotations.
   * <p>
   * Every bean is asked for its class rather than for an instance: nothing is created here, and
   * a class carrying a reserved attribute is refused before the application serves anything.
   *
   * @return The workflow services
   */
  private static Collection<Class<?>> workflowServiceClasses() {

    return Arc
        .container()
        .beanManager()
        .getBeans(Object.class, Any.Literal.INSTANCE)
        .stream()
        .map(bean -> (Class<?>) bean.getBeanClass())
        .filter(beanClass -> beanClass.isAnnotationPresent(WorkflowService.class))
        .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

  }

}
