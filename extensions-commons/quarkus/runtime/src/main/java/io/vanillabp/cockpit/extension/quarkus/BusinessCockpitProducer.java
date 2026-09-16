package io.vanillabp.cockpit.extension.quarkus;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
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
import io.vanillabp.cockpit.extension.transport.BusinessCockpitTransport;
import io.vanillabp.cockpit.extension.wiring.BusinessCockpitWiringService;
import io.vanillabp.integration.adapter.migration.config.MigrationAdapterProperties;
import io.vanillabp.integration.adapter.migration.processservice.PhaseTwoOutboxResolver;
import io.vanillabp.integration.adapter.migration.processservice.TransactionRunnerResolver;
import io.vanillabp.integration.extension.spi.ExtensionWiringService;
import io.vanillabp.integration.extension.spi.handler.ExtensionHandlers;
import io.vanillabp.integration.extension.spi.service.AggregateServiceFactory;
import io.vanillabp.integration.spi.PhaseOperationRegistry;
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
 * Registers the Business Cockpit extension on Quarkus. It is the twin of the Spring Boot
 * module's auto-configuration and does the same things with CDI.
 * <p>
 * The producers are <code>&#64;Singleton</code> and not <code>&#64;ApplicationScoped</code>.
 * What they produce has no no-argument constructor, so it cannot be proxied for a client.
 */
@ApplicationScoped
public class BusinessCockpitProducer {

  /**
   * When the workflow modules are registered while the application starts.
   * <p>
   * It has to run later than the startup observer which creates the outbox store's table.
   * VanillaBP's Quarkus integration runs that observer at
   * <code>VanillaBpDeploymentRunner.OUTBOX_DISPATCHER_STARTUP_PRIORITY</code>, which is the
   * priority <code>APPLICATION + 700</code>. An entry written any earlier would find no table.
   * The number is written out here and not read from the integration, because an extension does
   * not compile against a platform integration. This comment keeps the two in step, and so does
   * a test of the deployment module, which sees both numbers.
   */
  public static final int REGISTRATION_STARTUP_PRIORITY = Interceptor.Priority.APPLICATION + 800;

  /**
   * When the stores and transactions of the application's workflow aggregates are resolved.
   * <p>
   * It runs later than VanillaBP's deployment runner, which registers the workflow service of
   * every process service it builds. It runs earlier than the registration of the workflow
   * modules, which writes the first entries.
   */
  public static final int VALIDATION_STARTUP_PRIORITY = Interceptor.Priority.APPLICATION + 750;

  /**
   * What the application wrote about the Business Cockpit. It is read off this platform's
   * mapping and handed on as the neutral object which the core and every BPMS half read.
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
   * What the application configured, read and validated once while it boots.
   * <p>
   * It is a bean of its own. An application which brings a transport of its own may want to wrap
   * the shipped one, and
   * {@link BusinessCockpitAssembly#transportOf(BusinessCockpitConfiguration)} builds that from
   * this object.
   *
   * @param properties VanillaBP's resolved configuration
   * @param settings What the application wrote below the cockpit's own sections
   * @param transports Every bean of the transport type this application has
   * @return The configuration
   */
  @Produces
  @Singleton
  @Unremovable
  public BusinessCockpitConfiguration businessCockpitConfiguration(
      final MigrationAdapterProperties properties,
      final CockpitSettings settings,
      @Any final InjectableInstance<BusinessCockpitTransport> transports) {

    return BusinessCockpitConfiguration
        .readAndValidate(
            properties, settings, Templating.engineAvailable(), transportProvidedByTheApplication(
                transports));

  }

  /**
   * The transport the extension ships. An application replaces it by producing a bean of the
   * same type. <code>&#64;DefaultBean</code> is what ArC offers for the seam which Spring Boot's
   * <code>&#64;ConditionalOnMissingBean</code> makes on the other platform.
   * <p>
   * The seam sits at this point because of what such a transport inherits. It is called while an
   * outbox entry is dispatched, so a failure is repeated with a backoff and the call runs in the
   * transaction of the workflow aggregate. See decision 21 in the repository's DECISIONS.md.
   *
   * @param configuration The validated configuration
   * @return The transport
   */
  @Produces
  @Singleton
  @DefaultBean
  @Unremovable
  public BusinessCockpitTransport businessCockpitTransport(
      final BusinessCockpitConfiguration configuration) {

    return BusinessCockpitAssembly.transportOf(configuration);

  }

  /**
   * The extension itself, built from what the application configured.
   * <p>
   * It is also the {@link io.vanillabp.cockpit.extension.spi.BusinessCockpitEventPublisher}
   * which a BPMS half injects. There is one bean, so there is nothing to tell apart.
   *
   * @param configuration What the application configured
   * @param transport Where the reports go, the application's own bean where it has one
   * @param bridges The BPMS halves the application brought
   * @param bridgeLists The BPMS halves an extension produced as one list. That is how a BPMS
   *          half builds one bridge per configured adapter id on Quarkus. The configuration
   *          decides how many there are, so a producer method cannot
   * @param workflowModuleDetailsProviders What the application says about its modules
   * @param handlers VanillaBP's invocation of the details providers
   * @param outboxResolver VanillaBP's attribution of an outbox store to a workflow aggregate
   * @param transactionRunners VanillaBP's attribution of a transaction to a workflow aggregate
   * @return The extension
   */
  @Produces
  @Singleton
  @Unremovable
  public BusinessCockpitExtension businessCockpitExtension(
      final BusinessCockpitConfiguration configuration,
      final BusinessCockpitTransport transport,
      @Any final Instance<BusinessCockpitBpmsBridge> bridges,
      @Any final Instance<List<BusinessCockpitBpmsBridge>> bridgeLists,
      @Any final Instance<WorkflowModuleDetailsProvider> workflowModuleDetailsProviders,
      final ExtensionHandlers handlers,
      final PhaseTwoOutboxResolver outboxResolver,
      final TransactionRunnerResolver transactionRunners) {

    return new BusinessCockpitExtension(
        configuration, transport, theBridges(
            bridges, bridgeLists), workflowModuleDetailsProviders.stream().toList(), handlers, BusinessCockpitAssembly
                .templatingOf(configuration), theOutbox(outboxResolver), transactionRunners);

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
   * Resolves the outbox store and the transaction of every workflow aggregate. This is what
   * makes a store nobody can attribute end the boot instead of the first report.
   * <p>
   * It runs after the deployment pipeline. Only then has VanillaBP registered the workflow
   * services of every process service, and only then can it be asked which aggregate a BPMN
   * process works on.
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
   * The priority makes this run late. VanillaBP's outbox store creates its table in a startup
   * observer of its own, after the deployment pipeline noted which workflow modules started. An
   * entry written any earlier would find no table.
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
   * Whether the application brought a transport of its own. The configuration check needs that
   * answer, because such an application configures neither of the shipped transports and starts
   * all the same.
   * <p>
   * Which beans of that type this application has was decided while the application was built,
   * and so was which of them this class produces as the default. Asking the container reads that
   * answer and creates none of the beans. That matters twice. The shipped transport loads the
   * Kafka client which the application may not have, and it is built from the very configuration
   * being read here.
   *
   * @param transports Every bean of the transport type
   * @return Whether one of them is not the shipped default
   */
  private static boolean transportProvidedByTheApplication(
      final InjectableInstance<BusinessCockpitTransport> transports) {

    return transports
        .handlesStream()
        .map(InstanceHandle::getBean)
        .filter(Objects::nonNull)
        .anyMatch(bean -> !bean.isDefaultBean());

  }

  /**
   * Every BPMS half of this application, however it was produced.
   * <p>
   * A BPMS half which serves several configured adapter ids of its BPMS cannot say at build time
   * how many bridges that is, so it produces them as one list. VanillaBP's own Quarkus
   * integration collects its adapter deployment services in the same shape.
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
   * Where the extension writes its entries. See decision 12 in the repository's DECISIONS.md.
   * <p>
   * VanillaBP attributes the store to a workflow aggregate, so an entry of the extension lands
   * where an entry of the core lands. That is the store which the aggregate's transaction
   * reaches. Only the Quarkus integration knows which of the platform's default stores serves
   * which persistence, and whether it is usable at all. An extension which asks the resolver
   * gets that answer without compiling against the integration (decision 2). The resolver also
   * answers which stores the application holds, for the same reason. A default store which is
   * switched off or has no datasource is a bean here, but the platform would never pick it.

   */
  private static BusinessCockpitOutbox theOutbox(
      final PhaseTwoOutboxResolver outboxResolver) {

    return new BusinessCockpitOutbox(
        outboxResolver::resolveFor, outboxResolver::allStores, outboxResolver.remediesDescription());

  }


}
