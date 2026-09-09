package io.vanillabp.cockpit.extension.quarkus;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
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
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;

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
   * compile against a platform integration, so the two are kept in step by this comment - and
   * by a test of the deployment module, which does see both numbers.
   */
  public static final int REGISTRATION_STARTUP_PRIORITY = Interceptor.Priority.APPLICATION + 800;

  /**
   * When the stores and transactions of the application's workflow aggregates are resolved.
   * <p>
   * Later than VanillaBP's deployment runner, which registers the workflow service of every
   * process service it builds, and earlier than the registration of the workflow modules, which
   * writes the first entries.
   */
  public static final int VALIDATION_STARTUP_PRIORITY = Interceptor.Priority.APPLICATION + 750;

  /**
   * What the application wrote about the Business Cockpit, read off this platform's mapping and
   * handed on as the neutral object the core and every BPMS half read.
   *
   * @param overlay The cockpit's overlay of the shared <code>vanillabp.*</code> tree
   * @return The settings
   */
  @Produces
  @Singleton
  @Unremovable
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
   * @param properties VanillaBP's resolved configuration
   * @param settings What the application wrote below the cockpit's own sections
   * @param bridges The BPMS halves the application brought
   * @param bridgeLists The BPMS halves an extension produced as one list, which is how a BPMS
   *          half builds a bridge per configured adapter id on Quarkus: how many there are is
   *          decided by the configuration and therefore not by a producer method
   * @param workflowModuleDetailsProviders What the application says about its modules
   * @param handlers VanillaBP's invocation of the details providers
   * @param outboxResolver VanillaBP's attribution of an outbox store to a workflow aggregate
   * @param outboxes The outbox stores of the application
   * @param outboxAwares The stores an application named for single workflow aggregates
   * @param transactionRunners VanillaBP's attribution of a transaction to a workflow aggregate
   * @return The extension
   */
  @Produces
  @Singleton
  @Unremovable
  public BusinessCockpitExtension businessCockpitExtension(
      final MigrationAdapterProperties properties,
      final CockpitSettings settings,
      @Any final Instance<BusinessCockpitBpmsBridge> bridges,
      @Any final Instance<List<BusinessCockpitBpmsBridge>> bridgeLists,
      @Any final Instance<WorkflowModuleDetailsProvider> workflowModuleDetailsProviders,
      final ExtensionHandlers handlers,
      final PhaseTwoOutboxResolver outboxResolver,
      @Any final Instance<PhaseTwoOutbox> outboxes,
      @Any final Instance<PhaseTwoOutboxAware<?>> outboxAwares,
      final TransactionRunnerResolver transactionRunners) {

    final var configuration = BusinessCockpitConfiguration
        .readAndValidate(properties, settings, Templating.engineAvailable());
    return new BusinessCockpitExtension(
        configuration, BusinessCockpitAssembly.transportOf(configuration), theBridges(
            bridges, bridgeLists), workflowModuleDetailsProviders.stream().toList(), handlers, BusinessCockpitAssembly
                .templatingOf(configuration), theOutbox(
                    outboxResolver, outboxes, outboxAwares), transactionRunners);

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
   * Resolves the outbox store and the transaction of every workflow aggregate, which is what
   * makes a store nobody can attribute end the boot rather than the first report.
   * <p>
   * It runs after the deployment pipeline because that is when VanillaBP has registered the
   * workflow services of every process service, and the aggregate a BPMN process works on is
   * what it is asked for.
   *
   * @param event The startup
   * @param extension The extension
   */
  void validateWhereEntriesAreWritten(
      @Observes
      @Priority(VALIDATION_STARTUP_PRIORITY) final StartupEvent event,
      final BusinessCockpitExtension extension) {

    extension.validateWhereEntriesAreWritten();

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
   * Where the extension writes its entries - see decision 12 in the repository's DECISIONS.md.
   * <p>
   * The attribution of a store to a workflow aggregate is VanillaBP's own, so that an entry of
   * the extension lands where an entry of the core lands: in the store the aggregate's
   * transaction reaches. Which of the platform's default stores serves which persistence, and
   * whether it is usable at all, is knowledge of the Quarkus integration, and an extension
   * asking the resolver gets that answer without compiling against it (decision 2).
   */
  private static BusinessCockpitOutbox theOutbox(
      final PhaseTwoOutboxResolver outboxResolver,
      final Instance<PhaseTwoOutbox> outboxes,
      final Instance<PhaseTwoOutboxAware<?>> outboxAwares) {

    return new BusinessCockpitOutbox(
        outboxResolver::resolveFor, () -> storesOf(outboxes, outboxAwares), outboxResolver.remediesDescription());

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


}
