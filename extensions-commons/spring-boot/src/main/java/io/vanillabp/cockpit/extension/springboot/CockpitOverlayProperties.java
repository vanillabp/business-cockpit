package io.vanillabp.cockpit.extension.springboot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.vanillabp.cockpit.extension.config.CockpitSettings;
import io.vanillabp.integration.adapter.migration.config.MigrationAdapterProperties;

/**
 * The Business Cockpit's OVERLAY of the shared <code>vanillabp.*</code> configuration tree on
 * Spring Boot: a second {@code @ConfigurationProperties("vanillabp")} class next to the
 * platform's own binding, which is the pattern every adapter and extension of VanillaBP uses
 * to contribute keys of its own. Same-prefix classes bind side by side, and a key unknown to
 * one of them is ignored by that one.
 * <p>
 * The shape is the one version 1 of the Business Cockpit had, so an application upgrades by
 * changing a dependency rather than by rewriting its configuration:
 *
 * <pre>
 * vanillabp.cockpit.&lt;key&gt;
 * vanillabp.workflow-modules.&lt;id&gt;.cockpit.&lt;key&gt;
 * vanillabp.workflow-modules.&lt;id&gt;.workflows.&lt;process&gt;.cockpit.&lt;key&gt;
 * vanillabp.workflow-modules.&lt;id&gt;.workflows.&lt;process&gt;.user-tasks.&lt;task&gt;.cockpit.&lt;key&gt;
 * </pre>
 *
 * Every leaf is bound as text and parsed by the neutral core, so a value which is no number,
 * no span of time and no boolean is answered by the same message on both platforms. Lists and
 * maps are the exception: they are what a platform binds and an extension cannot reassemble
 * from text, so <code>i18n-languages</code> is a list here and the group hierarchy is a map of
 * lists, written the way version 1 wrote them.
 * <p>
 * The workflow modules of the application are never taken from this map - they come from the
 * platform's own properties, and this is a lookup per module id.
 */
@ConfigurationProperties(MigrationAdapterProperties.PREFIX)
public class CockpitOverlayProperties {

  /** What the whole application says about the Business Cockpit. */
  private Cockpit cockpit;

  /** What single workflow modules say, by workflow module id. */
  private Map<String, WorkflowModuleOverlay> workflowModules = Map.of();

  public Cockpit getCockpit() {

    return cockpit;

  }

  public void setCockpit(
      final Cockpit cockpit) {

    this.cockpit = cockpit;

  }

  public Map<String, WorkflowModuleOverlay> getWorkflowModules() {

    return workflowModules;

  }

  public void setWorkflowModules(
      final Map<String, WorkflowModuleOverlay> workflowModules) {

    this.workflowModules = workflowModules;

  }

  /**
   * What was bound, as the neutral core reads it.
   *
   * @return The settings of this application
   */
  public CockpitSettings toSettings() {

    final var modules = new LinkedHashMap<String, CockpitSettings.WorkflowModule>();
    workflowModules
        .forEach((
            workflowModuleId,
            module) -> modules.put(workflowModuleId, module.toSettings()));
    if (cockpit == null) {
      return new CockpitSettings(null, null, null, null, null, null, modules);
    }
    return new CockpitSettings(
        cockpit.getUserTasksEnabled(), cockpit.getWorkflowListEnabled(), cockpit
            .getTemplateLoaderPath(), restOf(cockpit.getRest()), kafkaOf(
                cockpit.getKafka()), cockpit.getProcessEngineApi() == null
                    ? null
                    : new CockpitSettings.ProcessEngineApi(
                        cockpit.getProcessEngineApi().getRememberedUserTasks()), modules);

  }

  private static CockpitSettings.Rest restOf(
      final Rest rest) {

    if (rest == null) {
      return null;
    }
    return new CockpitSettings.Rest(
        rest.getBaseUrl(), rest.getConnectTimeout(), rest.getReadTimeout(), rest.getVerifySsl(), rest
            .getSslTruststoreFilename(), rest
                .getSslTruststorePassword(), proxyOf(rest.getProxy()), authenticationOf(rest.getAuthentication()));

  }

  private static CockpitSettings.Authentication authenticationOf(
      final Authentication authentication) {

    if (authentication == null) {
      return null;
    }
    final var oauth = authentication.getOauth();
    return new CockpitSettings.Authentication(
        authentication.getBasic(), authentication.getUsername(), authentication.getPassword(), oauth == null
            ? null
            : new CockpitSettings.OAuth(
                oauth.getBaseUrl(), oauth.getClientId(), oauth.getClientSecret(), oauth.getBasic(), oauth
                    .getConnectTimeout(), oauth.getReadTimeout(), oauth.getVerifySsl(), oauth
                        .getSslTruststoreFilename(), oauth.getSslTruststorePassword(), proxyOf(oauth.getProxy())));

  }

  private static CockpitSettings.Proxy proxyOf(
      final Proxy proxy) {

    return proxy == null
        ? null
        : new CockpitSettings.Proxy(
            proxy.getHost(), proxy.getPort(), proxy.getUsername(), proxy.getPassword());

  }

  private static CockpitSettings.Kafka kafkaOf(
      final Kafka kafka) {

    if (kafka == null) {
      return null;
    }
    final var topics = kafka.getTopics();
    return new CockpitSettings.Kafka(
        kafka.getBootstrapServers(), topics == null
            ? null
            : new CockpitSettings.Topics(
                topics.getUserTask(), topics.getWorkflow(), topics.getWorkflowModule()), kafka.getProperties());

  }

  /**
   * Everything below <code>vanillabp.cockpit</code>.
   */
  public static class Cockpit {

    /** Whether user tasks are reported at all. */
    private String userTasksEnabled;

    /** Whether workflows are reported at all. */
    private String workflowListEnabled;

    /** The directory the templates are loaded from. */
    private String templateLoaderPath;

    /** The connection to the cockpit server, and with it the REST transport. */
    private Rest rest;

    /** The brokers the events are sent to, and with them the Kafka transport. */
    private Kafka kafka;

    /** What the cockpit's Process-Engine-API half reads. */
    private ProcessEngineApi processEngineApi;

    public String getUserTasksEnabled() {

      return userTasksEnabled;

    }

    public void setUserTasksEnabled(
        final String userTasksEnabled) {

      this.userTasksEnabled = userTasksEnabled;

    }

    public String getWorkflowListEnabled() {

      return workflowListEnabled;

    }

    public void setWorkflowListEnabled(
        final String workflowListEnabled) {

      this.workflowListEnabled = workflowListEnabled;

    }

    public String getTemplateLoaderPath() {

      return templateLoaderPath;

    }

    public void setTemplateLoaderPath(
        final String templateLoaderPath) {

      this.templateLoaderPath = templateLoaderPath;

    }

    public Rest getRest() {

      return rest;

    }

    public void setRest(
        final Rest rest) {

      this.rest = rest;

    }

    public Kafka getKafka() {

      return kafka;

    }

    public void setKafka(
        final Kafka kafka) {

      this.kafka = kafka;

    }

    public ProcessEngineApi getProcessEngineApi() {

      return processEngineApi;

    }

    public void setProcessEngineApi(
        final ProcessEngineApi processEngineApi) {

      this.processEngineApi = processEngineApi;

    }

  }

  /**
   * Everything below <code>vanillabp.cockpit.rest</code>.
   */
  public static class Rest {

    /** The cockpit server's address, and with it the choice of this transport. */
    private String baseUrl;

    /** How long establishing the connection may take. */
    private String connectTimeout;

    /** How long the server may take to answer. */
    private String readTimeout;

    /** Whether the certificate the server presents is checked at all. */
    private String verifySsl;

    /** The PKCS12 file the certificate is checked against. */
    private String sslTruststoreFilename;

    /** The password of that file. */
    private String sslTruststorePassword;

    /** The HTTP proxy the cockpit server is reached through. */
    private Proxy proxy;

    /** How the workflow module identifies itself. */
    private Authentication authentication;

    public String getBaseUrl() {

      return baseUrl;

    }

    public void setBaseUrl(
        final String baseUrl) {

      this.baseUrl = baseUrl;

    }

    public String getConnectTimeout() {

      return connectTimeout;

    }

    public void setConnectTimeout(
        final String connectTimeout) {

      this.connectTimeout = connectTimeout;

    }

    public String getReadTimeout() {

      return readTimeout;

    }

    public void setReadTimeout(
        final String readTimeout) {

      this.readTimeout = readTimeout;

    }

    public String getVerifySsl() {

      return verifySsl;

    }

    public void setVerifySsl(
        final String verifySsl) {

      this.verifySsl = verifySsl;

    }

    public String getSslTruststoreFilename() {

      return sslTruststoreFilename;

    }

    public void setSslTruststoreFilename(
        final String sslTruststoreFilename) {

      this.sslTruststoreFilename = sslTruststoreFilename;

    }

    public String getSslTruststorePassword() {

      return sslTruststorePassword;

    }

    public void setSslTruststorePassword(
        final String sslTruststorePassword) {

      this.sslTruststorePassword = sslTruststorePassword;

    }

    public Proxy getProxy() {

      return proxy;

    }

    public void setProxy(
        final Proxy proxy) {

      this.proxy = proxy;

    }

    public Authentication getAuthentication() {

      return authentication;

    }

    public void setAuthentication(
        final Authentication authentication) {

      this.authentication = authentication;

    }

  }

  /**
   * Everything below <code>rest.authentication</code>.
   */
  public static class Authentication {

    /** Whether a basic authentication is sent, which is version 1's switch. */
    private String basic;

    /** The user it is sent with. */
    private String username;

    /** The password belonging to the user. */
    private String password;

    /** The client-credentials flow the bearer token is fetched with. */
    private OAuth oauth;

    public String getBasic() {

      return basic;

    }

    public void setBasic(
        final String basic) {

      this.basic = basic;

    }

    public String getUsername() {

      return username;

    }

    public void setUsername(
        final String username) {

      this.username = username;

    }

    public String getPassword() {

      return password;

    }

    public void setPassword(
        final String password) {

      this.password = password;

    }

    public OAuth getOauth() {

      return oauth;

    }

    public void setOauth(
        final OAuth oauth) {

      this.oauth = oauth;

    }

  }

  /**
   * Everything below <code>rest.authentication.oauth</code>, which configures its own
   * connection the way version 1 did.
   */
  public static class OAuth {

    /** Where tokens are issued, and with it the choice of this flow. */
    private String baseUrl;

    /** The client the token is asked for. */
    private String clientId;

    /** The secret belonging to it. */
    private String clientSecret;

    /** Whether the client identifies itself in an Authorization header. */
    private String basic;

    /** How long establishing the connection may take. */
    private String connectTimeout;

    /** How long the authorization server may take to answer. */
    private String readTimeout;

    /** Whether its certificate is checked at all. */
    private String verifySsl;

    /** The PKCS12 file its certificate is checked against. */
    private String sslTruststoreFilename;

    /** The password of that file. */
    private String sslTruststorePassword;

    /** The HTTP proxy the authorization server is reached through. */
    private Proxy proxy;

    public String getBaseUrl() {

      return baseUrl;

    }

    public void setBaseUrl(
        final String baseUrl) {

      this.baseUrl = baseUrl;

    }

    public String getClientId() {

      return clientId;

    }

    public void setClientId(
        final String clientId) {

      this.clientId = clientId;

    }

    public String getClientSecret() {

      return clientSecret;

    }

    public void setClientSecret(
        final String clientSecret) {

      this.clientSecret = clientSecret;

    }

    public String getBasic() {

      return basic;

    }

    public void setBasic(
        final String basic) {

      this.basic = basic;

    }

    public String getConnectTimeout() {

      return connectTimeout;

    }

    public void setConnectTimeout(
        final String connectTimeout) {

      this.connectTimeout = connectTimeout;

    }

    public String getReadTimeout() {

      return readTimeout;

    }

    public void setReadTimeout(
        final String readTimeout) {

      this.readTimeout = readTimeout;

    }

    public String getVerifySsl() {

      return verifySsl;

    }

    public void setVerifySsl(
        final String verifySsl) {

      this.verifySsl = verifySsl;

    }

    public String getSslTruststoreFilename() {

      return sslTruststoreFilename;

    }

    public void setSslTruststoreFilename(
        final String sslTruststoreFilename) {

      this.sslTruststoreFilename = sslTruststoreFilename;

    }

    public String getSslTruststorePassword() {

      return sslTruststorePassword;

    }

    public void setSslTruststorePassword(
        final String sslTruststorePassword) {

      this.sslTruststorePassword = sslTruststorePassword;

    }

    public Proxy getProxy() {

      return proxy;

    }

    public void setProxy(
        final Proxy proxy) {

      this.proxy = proxy;

    }

  }

  /**
   * An HTTP proxy, below one of the two connections.
   */
  public static class Proxy {

    /** The proxy's host, which is what switches it on. */
    private String host;

    /** The proxy's port. */
    private String port;

    /** The user it expects, where it expects one. */
    private String username;

    /** The password belonging to the user. */
    private String password;

    public String getHost() {

      return host;

    }

    public void setHost(
        final String host) {

      this.host = host;

    }

    public String getPort() {

      return port;

    }

    public void setPort(
        final String port) {

      this.port = port;

    }

    public String getUsername() {

      return username;

    }

    public void setUsername(
        final String username) {

      this.username = username;

    }

    public String getPassword() {

      return password;

    }

    public void setPassword(
        final String password) {

      this.password = password;

    }

  }

  /**
   * Everything below <code>vanillabp.cockpit.kafka</code>.
   */
  public static class Kafka {

    /** The brokers, and with them the choice of this transport. */
    private String bootstrapServers;

    /** One topic per kind of event. */
    private Topics topics;

    /** Anything else the producer is to be given. */
    private Map<String, String> properties;

    public String getBootstrapServers() {

      return bootstrapServers;

    }

    public void setBootstrapServers(
        final String bootstrapServers) {

      this.bootstrapServers = bootstrapServers;

    }

    public Topics getTopics() {

      return topics;

    }

    public void setTopics(
        final Topics topics) {

      this.topics = topics;

    }

    public Map<String, String> getProperties() {

      return properties;

    }

    public void setProperties(
        final Map<String, String> properties) {

      this.properties = properties;

    }

  }

  /**
   * The three topics the cockpit server reads.
   */
  public static class Topics {

    /** The topic user-task events are sent to. */
    private String userTask;

    /** The topic workflow events are sent to. */
    private String workflow;

    /** The topic a workflow module's registration is sent to. */
    private String workflowModule;

    public String getUserTask() {

      return userTask;

    }

    public void setUserTask(
        final String userTask) {

      this.userTask = userTask;

    }

    public String getWorkflow() {

      return workflow;

    }

    public void setWorkflow(
        final String workflow) {

      this.workflow = workflow;

    }

    public String getWorkflowModule() {

      return workflowModule;

    }

    public void setWorkflowModule(
        final String workflowModule) {

      this.workflowModule = workflowModule;

    }

  }

  /**
   * The one setting of the cockpit's Process-Engine-API half.
   */
  public static class ProcessEngineApi {

    /** How many delivered user tasks a node remembers. */
    private String rememberedUserTasks;

    public String getRememberedUserTasks() {

      return rememberedUserTasks;

    }

    public void setRememberedUserTasks(
        final String rememberedUserTasks) {

      this.rememberedUserTasks = rememberedUserTasks;

    }

  }

  /**
   * The cockpit's view of one <code>vanillabp.workflow-modules.&lt;id&gt;</code> section.
   */
  public static class WorkflowModuleOverlay {

    /** What the module itself says, below <code>cockpit</code>. */
    private ModuleCockpit cockpit;

    /** What single workflows of it say, by BPMN process id. */
    private Map<String, WorkflowOverlay> workflows = Map.of();

    public ModuleCockpit getCockpit() {

      return cockpit;

    }

    public void setCockpit(
        final ModuleCockpit cockpit) {

      this.cockpit = cockpit;

    }

    public Map<String, WorkflowOverlay> getWorkflows() {

      return workflows;

    }

    public void setWorkflows(
        final Map<String, WorkflowOverlay> workflows) {

      this.workflows = workflows;

    }


    /**
     * @return What this workflow module said, as the neutral core reads it
     */
    public CockpitSettings.WorkflowModule toSettings() {

      final var ofTheWorkflows = new LinkedHashMap<String, CockpitSettings.Workflow>();
      workflows
          .forEach((
              bpmnProcessId,
              workflow) -> {
            final var ofTheWorkflow = workflow.toSettings();
            if (ofTheWorkflow != null) {
              ofTheWorkflows.put(bpmnProcessId, ofTheWorkflow);
            }
          });
      return new CockpitSettings.WorkflowModule(cockpitOf(cockpit), ofTheWorkflows);

    }

    /**
     * A module which wrote nothing takes no part in the Business Cockpit, and a module which
     * wrote an empty section wrote nothing.
     *
     * @param cockpit What was bound below the module's <code>cockpit</code>
     * @return The section as the neutral core reads it, or <code>null</code>
     */
    private static CockpitSettings.Cockpit cockpitOf(
        final ModuleCockpit cockpit) {

      if (cockpit == null) {
        return null;
      }
      if ((cockpit.getWorkflowModuleUri() == null) && (cockpit.getUiUriType() == null) && (cockpit
          .getUiUriPath() == null) && (cockpit.getI18nLanguages() == null) && (cockpit
              .getBpmnDescriptionLanguage() == null) && (cockpit
                  .getTemplatePath() == null) && ((cockpit.getGroupHierarchy() == null) || cockpit.getGroupHierarchy()
                      .isEmpty())) {
        return null;
      }
      return new CockpitSettings.Cockpit(
          cockpit.getWorkflowModuleUri(), cockpit.getUiUriType(), cockpit.getUiUriPath(), cockpit
              .getI18nLanguages(), cockpit
                  .getBpmnDescriptionLanguage(), cockpit.getTemplatePath(), cockpit.getGroupHierarchy());

    }

  }

  /**
   * Everything below <code>vanillabp.workflow-modules.&lt;id&gt;.cockpit</code>.
   */
  public static class ModuleCockpit {

    /** Where the module answers the cockpit's provider APIs. */
    private String workflowModuleUri;

    /** How the cockpit loads this module's forms. */
    private String uiUriType;

    /** Where this module's forms are served from. */
    private String uiUriPath;

    /** The languages titles are reported in. */
    private List<String> i18nLanguages;

    /** The language the BPMN names are written in. */
    private String bpmnDescriptionLanguage;

    /** The segment this module contributes to the template lookup path. */
    private String templatePath;

    /** Which groups a group stands for. */
    private Map<String, List<String>> groupHierarchy;

    public String getWorkflowModuleUri() {

      return workflowModuleUri;

    }

    public void setWorkflowModuleUri(
        final String workflowModuleUri) {

      this.workflowModuleUri = workflowModuleUri;

    }

    public String getUiUriType() {

      return uiUriType;

    }

    public void setUiUriType(
        final String uiUriType) {

      this.uiUriType = uiUriType;

    }

    public String getUiUriPath() {

      return uiUriPath;

    }

    public void setUiUriPath(
        final String uiUriPath) {

      this.uiUriPath = uiUriPath;

    }

    public List<String> getI18nLanguages() {

      return i18nLanguages;

    }

    public void setI18nLanguages(
        final List<String> i18nLanguages) {

      this.i18nLanguages = i18nLanguages;

    }

    public String getBpmnDescriptionLanguage() {

      return bpmnDescriptionLanguage;

    }

    public void setBpmnDescriptionLanguage(
        final String bpmnDescriptionLanguage) {

      this.bpmnDescriptionLanguage = bpmnDescriptionLanguage;

    }

    public String getTemplatePath() {

      return templatePath;

    }

    public void setTemplatePath(
        final String templatePath) {

      this.templatePath = templatePath;

    }

    public Map<String, List<String>> getGroupHierarchy() {

      return groupHierarchy;

    }

    public void setGroupHierarchy(
        final Map<String, List<String>> groupHierarchy) {

      this.groupHierarchy = groupHierarchy;

    }

  }

  /**
   * The cockpit's view of one workflow of a workflow module.
   */
  public static class WorkflowOverlay {

    /** What this workflow says, below <code>cockpit</code>. */
    private WorkflowCockpit cockpit;

    /** What single user tasks of it say, by task definition. */
    private Map<String, UserTaskOverlay> userTasks = Map.of();

    public WorkflowCockpit getCockpit() {

      return cockpit;

    }

    public void setCockpit(
        final WorkflowCockpit cockpit) {

      this.cockpit = cockpit;

    }

    public Map<String, UserTaskOverlay> getUserTasks() {

      return userTasks;

    }

    public void setUserTasks(
        final Map<String, UserTaskOverlay> userTasks) {

      this.userTasks = userTasks;

    }


    /**
     * The workflows of a workflow module are the platform's as well, and a workflow which says
     * nothing about the cockpit is no workflow of the cockpit.
     *
     * @return What this workflow said, as the neutral core reads it, or <code>null</code> where
     *         it said nothing
     */
    public CockpitSettings.Workflow toSettings() {

      final var ofTheUserTasks = new LinkedHashMap<String, CockpitSettings.UserTask>();
      userTasks
          .forEach((
              taskDefinition,
              userTask) -> {
            final var templatePath = userTask.getCockpit() == null
                ? null
                : userTask.getCockpit().getTemplatePath();
            if (templatePath != null) {
              ofTheUserTasks.put(taskDefinition, new CockpitSettings.UserTask(templatePath));
            }
          });
      if (saysNothing() && ofTheUserTasks.isEmpty()) {
        return null;
      }
      return new CockpitSettings.Workflow(
          cockpit == null ? null : cockpit.getI18nLanguages(), cockpit == null
              ? null
              : cockpit.getBpmnDescriptionLanguage(), cockpit == null ? null
                  : cockpit.getTemplatePath(), ofTheUserTasks);

    }

    private boolean saysNothing() {

      return (cockpit == null) || ((cockpit.getI18nLanguages() == null) && (cockpit
          .getBpmnDescriptionLanguage() == null) && (cockpit.getTemplatePath() == null));

    }

  }

  /**
   * Everything below <code>…workflows.&lt;process&gt;.cockpit</code>.
   */
  public static class WorkflowCockpit {

    /** The languages this workflow's titles are reported in. */
    private List<String> i18nLanguages;

    /** The language this workflow's BPMN names are written in. */
    private String bpmnDescriptionLanguage;

    /** The segment this workflow contributes to the template lookup path. */
    private String templatePath;

    public List<String> getI18nLanguages() {

      return i18nLanguages;

    }

    public void setI18nLanguages(
        final List<String> i18nLanguages) {

      this.i18nLanguages = i18nLanguages;

    }

    public String getBpmnDescriptionLanguage() {

      return bpmnDescriptionLanguage;

    }

    public void setBpmnDescriptionLanguage(
        final String bpmnDescriptionLanguage) {

      this.bpmnDescriptionLanguage = bpmnDescriptionLanguage;

    }

    public String getTemplatePath() {

      return templatePath;

    }

    public void setTemplatePath(
        final String templatePath) {

      this.templatePath = templatePath;

    }

  }

  /**
   * The cockpit's view of one user task of a workflow.
   */
  public static class UserTaskOverlay {

    /** What this user task says, below <code>cockpit</code>. */
    private UserTaskCockpit cockpit;

    public UserTaskCockpit getCockpit() {

      return cockpit;

    }

    public void setCockpit(
        final UserTaskCockpit cockpit) {

      this.cockpit = cockpit;

    }

  }

  /**
   * Everything below <code>…user-tasks.&lt;task&gt;.cockpit</code>.
   */
  public static class UserTaskCockpit {

    /** The segment this user task contributes to the template lookup path. */
    private String templatePath;

    public String getTemplatePath() {

      return templatePath;

    }

    public void setTemplatePath(
        final String templatePath) {

      this.templatePath = templatePath;

    }

  }

}
