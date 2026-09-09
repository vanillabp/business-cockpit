package io.vanillabp.cockpit.extension.quarkus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.vanillabp.cockpit.extension.config.CockpitSettings;

/**
 * The Business Cockpit's OVERLAY of the shared <code>vanillabp.*</code> configuration tree on
 * Quarkus: a second RUN_TIME mapping over the same prefix as the platform's own, which is the
 * pattern every VanillaBP adapter and extension uses to contribute keys of its own. Overlapping
 * keys are served to both mappings, and SmallRye's unknown-key validation passes as soon as one
 * of them declares a key.
 * <p>
 * On this platform the mapping is not merely how the keys are read - it is what lets the
 * application boot at all. A key below <code>vanillabp</code> which no mapping declares ends
 * the startup, so everything the Business Cockpit reads is declared here, in the shape version
 * 1 of the cockpit had:
 *
 * <pre>
 * vanillabp.cockpit.&lt;key&gt;
 * vanillabp.workflow-modules.&lt;id&gt;.cockpit.&lt;key&gt;
 * vanillabp.workflow-modules.&lt;id&gt;.workflows.&lt;process&gt;.cockpit.&lt;key&gt;
 * vanillabp.workflow-modules.&lt;id&gt;.workflows.&lt;process&gt;.user-tasks.&lt;task&gt;.cockpit.&lt;key&gt;
 * </pre>
 *
 * Every leaf is declared as text and parsed by the neutral core, so a value which is no number,
 * no span of time and no boolean is answered by the same message here and on Spring Boot rather
 * than by two frameworks in two ways. Lists and maps are the exception: they are what a
 * platform binds and an extension cannot reassemble from text.
 * <p>
 * The workflow modules of the application are never taken from this map - they come from the
 * platform's own properties, and this is a lookup per module id.
 * <p>
 * The mapping is deliberately NOT <code>&#64;StaticInitSafe</code>. SmallRye validates the
 * unknown keys of a root once per configuration it builds, against the mappings registered in
 * that one: an overlay which is created while the application's static initializer runs, next to
 * a platform mapping which is created when the application starts, is the only mapping of
 * <code>vanillabp</code> there - and every key of the platform below that root is then a key
 * nothing declares. An overlay belongs in the phase the mapping it overlays is in.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "vanillabp")
public interface CockpitOverlayProperties {

  /**
   * What the whole application says about the Business Cockpit.
   */
  Cockpit cockpit();

  /**
   * What single workflow modules say, by workflow module id.
   */
  Map<String, WorkflowModuleOverlay> workflowModules();

  /**
   * @return What was bound, as the neutral core reads it
   */
  default CockpitSettings toSettings() {

    final var modules = new LinkedHashMap<String, CockpitSettings.WorkflowModule>();
    workflowModules()
        .forEach((
            workflowModuleId,
            module) -> modules.put(workflowModuleId, module.toSettings()));
    return new CockpitSettings(
        cockpit().userTasksEnabled().orElse(null), cockpit().workflowListEnabled().orElse(null), cockpit()
            .templateLoaderPath().orElse(null), cockpit().rest()
                .toSettings(), cockpit().kafka().toSettings(), cockpit().processEngineApi().toSettings(), modules);

  }

  /**
   * Everything below <code>vanillabp.cockpit</code>.
   */
  interface Cockpit {

    /**
     * Whether user tasks are reported at all.
     */
    Optional<String> userTasksEnabled();

    /**
     * Whether workflows are reported at all.
     */
    Optional<String> workflowListEnabled();

    /**
     * The directory the templates are loaded from.
     */
    Optional<String> templateLoaderPath();

    /**
     * The connection to the cockpit server, and with it the REST transport.
     */
    Rest rest();

    /**
     * The brokers the events are sent to, and with them the Kafka transport.
     */
    Kafka kafka();

    /**
     * What the cockpit's Process-Engine-API half reads.
     */
    ProcessEngineApi processEngineApi();

  }

  /**
   * Everything below <code>vanillabp.cockpit.rest</code>.
   */
  interface Rest {

    /**
     * The cockpit server's address, and with it the choice of this transport.
     */
    Optional<String> baseUrl();

    /**
     * How long establishing the connection may take.
     */
    Optional<String> connectTimeout();

    /**
     * How long the server may take to answer.
     */
    Optional<String> readTimeout();

    /**
     * Whether the certificate the server presents is checked at all.
     */
    Optional<String> verifySsl();

    /**
     * The PKCS12 file the certificate is checked against.
     */
    Optional<String> sslTruststoreFilename();

    /**
     * The password of that file.
     */
    Optional<String> sslTruststorePassword();

    /**
     * The HTTP proxy the cockpit server is reached through.
     */
    Proxy proxy();

    /**
     * How the workflow module identifies itself.
     */
    Authentication authentication();

    /**
     * @return This section as the neutral core reads it
     */
    default CockpitSettings.Rest toSettings() {

      final var proxy = proxy().toSettings();
      final var authentication = authentication().toSettings();
      if (baseUrl().isEmpty() && connectTimeout().isEmpty() && readTimeout().isEmpty() && verifySsl()
          .isEmpty() && sslTruststoreFilename()
              .isEmpty() && sslTruststorePassword().isEmpty() && (proxy == null) && (authentication == null)) {
        return null;
      }
      return new CockpitSettings.Rest(
          baseUrl().orElse(null), connectTimeout().orElse(null), readTimeout().orElse(null), verifySsl().orElse(
              null), sslTruststoreFilename().orElse(null), sslTruststorePassword().orElse(null), proxy, authentication);

    }

  }

  /**
   * Everything below <code>rest.authentication</code>.
   */
  interface Authentication {

    /**
     * Whether a basic authentication is sent, which is version 1's switch.
     */
    Optional<String> basic();

    /**
     * The user it is sent with.
     */
    Optional<String> username();

    /**
     * The password belonging to the user.
     */
    Optional<String> password();

    /**
     * The client-credentials flow the bearer token is fetched with.
     */
    OAuth oauth();

    /**
     * @return This section as the neutral core reads it
     */
    default CockpitSettings.Authentication toSettings() {

      final var oauth = oauth().toSettings();
      if (basic().isEmpty() && username().isEmpty() && password().isEmpty() && (oauth == null)) {
        return null;
      }
      return new CockpitSettings.Authentication(
          basic().orElse(null), username().orElse(null), password().orElse(null), oauth);

    }

  }

  /**
   * Everything below <code>rest.authentication.oauth</code>, which configures its own
   * connection the way version 1 did.
   */
  interface OAuth {

    /**
     * Where tokens are issued, and with it the choice of this flow.
     */
    Optional<String> baseUrl();

    /**
     * The client the token is asked for.
     */
    Optional<String> clientId();

    /**
     * The secret belonging to it.
     */
    Optional<String> clientSecret();

    /**
     * Whether the client identifies itself in an <code>Authorization</code> header.
     */
    Optional<String> basic();

    /**
     * How long establishing the connection may take.
     */
    Optional<String> connectTimeout();

    /**
     * How long the authorization server may take to answer.
     */
    Optional<String> readTimeout();

    /**
     * Whether its certificate is checked at all.
     */
    Optional<String> verifySsl();

    /**
     * The PKCS12 file its certificate is checked against.
     */
    Optional<String> sslTruststoreFilename();

    /**
     * The password of that file.
     */
    Optional<String> sslTruststorePassword();

    /**
     * The HTTP proxy the authorization server is reached through.
     */
    Proxy proxy();

    /**
     * @return This section as the neutral core reads it
     */
    default CockpitSettings.OAuth toSettings() {

      final var proxy = proxy().toSettings();
      if (baseUrl().isEmpty() && clientId().isEmpty() && clientSecret().isEmpty() && basic()
          .isEmpty() && connectTimeout().isEmpty() && readTimeout().isEmpty() && verifySsl()
              .isEmpty() && sslTruststoreFilename().isEmpty() && sslTruststorePassword().isEmpty() && (proxy == null)) {
        return null;
      }
      return new CockpitSettings.OAuth(
          baseUrl().orElse(null), clientId().orElse(null), clientSecret().orElse(null), basic()
              .orElse(null), connectTimeout().orElse(null), readTimeout().orElse(null), verifySsl()
                  .orElse(null), sslTruststoreFilename().orElse(null), sslTruststorePassword().orElse(null), proxy);

    }

  }

  /**
   * An HTTP proxy, below one of the two connections.
   */
  interface Proxy {

    /**
     * The proxy's host, which is what switches it on.
     */
    Optional<String> host();

    /**
     * The proxy's port.
     */
    Optional<String> port();

    /**
     * The user it expects, where it expects one.
     */
    Optional<String> username();

    /**
     * The password belonging to the user.
     */
    Optional<String> password();

    /**
     * @return This section as the neutral core reads it
     */
    default CockpitSettings.Proxy toSettings() {

      if (host().isEmpty() && port().isEmpty() && username().isEmpty() && password().isEmpty()) {
        return null;
      }
      return new CockpitSettings.Proxy(
          host().orElse(null), port().orElse(null), username().orElse(null), password().orElse(null));

    }

  }

  /**
   * Everything below <code>vanillabp.cockpit.kafka</code>.
   */
  interface Kafka {

    /**
     * The brokers, and with them the choice of this transport.
     */
    Optional<String> bootstrapServers();

    /**
     * One topic per kind of event.
     */
    Topics topics();

    /**
     * Anything else the producer is to be given, e.g.
     * <code>kafka.properties.security.protocol</code>. A key of this map carries the dots it was
     * written with, which is what makes a setting of the Kafka client reach it unchanged.
     */
    Map<String, String> properties();

    /**
     * @return This section as the neutral core reads it
     */
    default CockpitSettings.Kafka toSettings() {

      final var topics = topics().toSettings();
      if (bootstrapServers().isEmpty() && (topics == null) && properties().isEmpty()) {
        return null;
      }
      return new CockpitSettings.Kafka(bootstrapServers().orElse(null), topics, properties());

    }

  }

  /**
   * The three topics the cockpit server reads.
   */
  interface Topics {

    /**
     * The topic user-task events are sent to.
     */
    Optional<String> userTask();

    /**
     * The topic workflow events are sent to.
     */
    Optional<String> workflow();

    /**
     * The topic a workflow module's registration is sent to.
     */
    Optional<String> workflowModule();

    /**
     * @return This section as the neutral core reads it
     */
    default CockpitSettings.Topics toSettings() {

      if (userTask().isEmpty() && workflow().isEmpty() && workflowModule().isEmpty()) {
        return null;
      }
      return new CockpitSettings.Topics(
          userTask().orElse(null), workflow().orElse(null), workflowModule().orElse(null));

    }

  }

  /**
   * The one setting of the cockpit's Process-Engine-API half, declared here because a key which
   * no mapping declares ends the boot on this platform.
   */
  interface ProcessEngineApi {

    /**
     * How many delivered user tasks a node remembers.
     */
    Optional<String> rememberedUserTasks();

    /**
     * @return This section as the neutral core reads it, or <code>null</code> where nothing was
     *         written into it
     */
    default CockpitSettings.ProcessEngineApi toSettings() {

      return rememberedUserTasks()
          .map(CockpitSettings.ProcessEngineApi::new)
          .orElse(null);

    }

  }

  /**
   * The cockpit's view of one <code>vanillabp.workflow-modules.&lt;id&gt;</code> section.
   */
  interface WorkflowModuleOverlay {

    /**
     * What the module itself says, below <code>cockpit</code>.
     */
    ModuleCockpit cockpit();

    /**
     * What single workflows of it say, by BPMN process id.
     */
    Map<String, WorkflowOverlay> workflows();

    /**
     * @return What this workflow module said, as the neutral core reads it
     */
    default CockpitSettings.WorkflowModule toSettings() {

      final var ofTheWorkflows = new LinkedHashMap<String, CockpitSettings.Workflow>();
      workflows()
          .forEach((
              bpmnProcessId,
              workflow) -> {
            final var ofTheWorkflow = workflow.toSettings();
            if (ofTheWorkflow != null) {
              ofTheWorkflows.put(bpmnProcessId, ofTheWorkflow);
            }
          });
      return new CockpitSettings.WorkflowModule(cockpit().toSettings(), ofTheWorkflows);

    }

  }

  /**
   * Everything below <code>vanillabp.workflow-modules.&lt;id&gt;.cockpit</code>.
   */
  interface ModuleCockpit {

    /**
     * Where the module answers the cockpit's provider APIs.
     */
    Optional<String> workflowModuleUri();

    /**
     * How the cockpit loads this module's forms.
     */
    Optional<String> uiUriType();

    /**
     * Where this module's forms are served from.
     */
    Optional<String> uiUriPath();

    /**
     * The languages titles are reported in.
     */
    Optional<List<String>> i18nLanguages();

    /**
     * The language the BPMN names are written in.
     */
    Optional<String> bpmnDescriptionLanguage();

    /**
     * The segment this module contributes to the template lookup path.
     */
    Optional<String> templatePath();

    /**
     * Which groups a group stands for.
     */
    Map<String, List<String>> groupHierarchy();

    /**
     * A module which wrote nothing takes no part in the Business Cockpit, and on this platform
     * that is what an absent section looks like: the groups of a mapping exist whether or not
     * anything was written into them, so the section is empty rather than missing.
     *
     * @return This section as the neutral core reads it, or <code>null</code> where the module
     *         wrote nothing at all
     */
    default CockpitSettings.Cockpit toSettings() {

      if (workflowModuleUri().isEmpty() && uiUriType().isEmpty() && uiUriPath().isEmpty() && i18nLanguages()
          .isEmpty() && bpmnDescriptionLanguage().isEmpty() && templatePath().isEmpty() && groupHierarchy().isEmpty()) {
        return null;
      }
      return new CockpitSettings.Cockpit(
          workflowModuleUri().orElse(null), uiUriType().orElse(null), uiUriPath().orElse(null), i18nLanguages()
              .orElse(null), bpmnDescriptionLanguage().orElse(null), templatePath().orElse(null), groupHierarchy());

    }

  }

  /**
   * The cockpit's view of one workflow of a workflow module.
   */
  interface WorkflowOverlay {

    /**
     * What this workflow says, below <code>cockpit</code>.
     */
    WorkflowCockpit cockpit();

    /**
     * What single user tasks of it say, by task definition.
     */
    Map<String, UserTaskOverlay> userTasks();

    /**
     * The workflows of a workflow module are the platform's as well, and a workflow which says
     * nothing about the cockpit is no workflow of the cockpit: this mapping gets an entry for
     * every workflow the application configured, whatever it configured there. A module whose
     * workflows only carry adapter keys would otherwise look like a module which takes part in
     * the cockpit, and the boot would end asking it for a workflow module URI.
     *
     * @return What this workflow said, as the neutral core reads it, or <code>null</code> where
     *         it said nothing
     */
    default CockpitSettings.Workflow toSettings() {

      final var ofTheUserTasks = new LinkedHashMap<String, CockpitSettings.UserTask>();
      userTasks()
          .forEach((
              taskDefinition,
              userTask) -> userTask
                  .cockpit()
                  .templatePath()
                  .ifPresent(
                      templatePath -> ofTheUserTasks
                          .put(taskDefinition, new CockpitSettings.UserTask(templatePath))));
      if (cockpit().i18nLanguages().isEmpty() && cockpit().bpmnDescriptionLanguage().isEmpty() && cockpit()
          .templatePath().isEmpty() && ofTheUserTasks.isEmpty()) {
        return null;
      }
      return new CockpitSettings.Workflow(
          cockpit().i18nLanguages().orElse(null), cockpit().bpmnDescriptionLanguage().orElse(null), cockpit()
              .templatePath().orElse(null), ofTheUserTasks);

    }

  }

  /**
   * Everything below <code>…workflows.&lt;process&gt;.cockpit</code>.
   */
  interface WorkflowCockpit {

    /**
     * The languages this workflow's titles are reported in.
     */
    Optional<List<String>> i18nLanguages();

    /**
     * The language this workflow's BPMN names are written in.
     */
    Optional<String> bpmnDescriptionLanguage();

    /**
     * The segment this workflow contributes to the template lookup path.
     */
    Optional<String> templatePath();

  }

  /**
   * The cockpit's view of one user task of a workflow.
   */
  interface UserTaskOverlay {

    /**
     * What this user task says, below <code>cockpit</code>.
     */
    UserTaskCockpit cockpit();

  }

  /**
   * Everything below <code>…user-tasks.&lt;task&gt;.cockpit</code>.
   */
  interface UserTaskCockpit {

    /**
     * The segment this user task contributes to the template lookup path.
     */
    Optional<String> templatePath();

  }

}
