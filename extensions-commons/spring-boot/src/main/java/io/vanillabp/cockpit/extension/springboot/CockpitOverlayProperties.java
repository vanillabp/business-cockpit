package io.vanillabp.cockpit.extension.springboot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.vanillabp.cockpit.extension.config.CockpitSettings;
import io.vanillabp.integration.adapter.migration.config.MigrationAdapterProperties;

/**
 * The Business Cockpit's OVERLAY of the shared <code>vanillabp.*</code> configuration tree on
 * Spring Boot. It is a second {@code @ConfigurationProperties("vanillabp")} class next to the
 * platform's own binding. Every adapter and extension of VanillaBP contributes keys of its own
 * in this way. Classes with the same prefix bind side by side, and a key which one of them does
 * not know is ignored by that one.
 * <p>
 * The shape is the one version 1 of the Business Cockpit had, so an application upgrades by
 * changing a dependency and not by rewriting its configuration:
 *
 * <pre>
 * vanillabp.cockpit.&lt;key&gt;
 * vanillabp.workflow-modules.&lt;id&gt;.cockpit.&lt;key&gt;
 * vanillabp.workflow-modules.&lt;id&gt;.workflows.&lt;process&gt;.cockpit.&lt;key&gt;
 * vanillabp.workflow-modules.&lt;id&gt;.workflows.&lt;process&gt;.user-tasks.&lt;task&gt;.cockpit.&lt;key&gt;
 * </pre>
 *
 * Every leaf is bound as text and parsed by the neutral core. A value which is no number, no
 * span of time and no boolean gets the same message on both platforms. Lists and maps are the
 * exception. A platform binds them, and an extension cannot reassemble them from text. So
 * <code>i18n-languages</code> is a list here, and the group hierarchy is a map of lists, written
 * the way version 1 wrote them.
 * <p>
 * The workflow modules of the application are never taken from this map. They come from the
 * platform's own properties, and this map is a lookup per module id.
 */
@ConfigurationProperties(MigrationAdapterProperties.PREFIX)
public class CockpitOverlayProperties {

  /** What the whole application says about the Business Cockpit. */
  private Cockpit cockpit;

  /** What single workflow modules say, by workflow module id. */
  private Map<String, WorkflowModuleOverlay> workflowModules = Map.of();

  /**
   * Creates the whole tree, which Spring Boot fills when it binds the properties.
   */
  public CockpitOverlayProperties() {
  }

  /**
   * What the whole application says about the Business Cockpit.
   *
   * @return The section below {@code vanillabp.cockpit}
   */
  public Cockpit getCockpit() {

    return cockpit;

  }

  /**
   * What the whole application says about the Business Cockpit.
   *
   * @param cockpit The section below {@code vanillabp.cockpit}
   */
  public void setCockpit(
      final Cockpit cockpit) {

    this.cockpit = cockpit;

  }

  /**
   * What single workflow modules say, by workflow module id.
   *
   * @return The sections of the workflow modules, by workflow module id
   */
  public Map<String, WorkflowModuleOverlay> getWorkflowModules() {

    return workflowModules;

  }

  /**
   * What single workflow modules say, by workflow module id.
   *
   * @param workflowModules The sections of the workflow modules, by workflow module id
   */
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
      return new CockpitSettings(null, null, null, null, null, null, null, modules);
    }
    return new CockpitSettings(
        cockpit.getUserTasksEnabled(), cockpit.getWorkflowListEnabled(), cockpit
            .getTemplateLoaderPath(), cockpit.getInitiatorSource(), restOf(cockpit.getRest()), kafkaOf(
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

    /** Who answers the initiator of a report. */
    private String initiatorSource;

    /** The connection to the cockpit server, and with it the REST transport. */
    private Rest rest;

    /** The brokers the events are sent to, and with them the Kafka transport. */
    private Kafka kafka;

    /** What the cockpit's Process-Engine-API half reads. */
    private ProcessEngineApi processEngineApi;

    /**
     * Creates an empty section, which Spring Boot fills when it binds the properties.
     */
    public Cockpit() {
    }

    /**
     * Whether user tasks are reported at all.
     *
     * @return The switch, as text
     */
    public String getUserTasksEnabled() {

      return userTasksEnabled;

    }

    /**
     * Whether user tasks are reported at all.
     *
     * @param userTasksEnabled The switch, as text
     */
    public void setUserTasksEnabled(
        final String userTasksEnabled) {

      this.userTasksEnabled = userTasksEnabled;

    }

    /**
     * Whether workflows are reported at all.
     *
     * @return The switch, as text
     */
    public String getWorkflowListEnabled() {

      return workflowListEnabled;

    }

    /**
     * Whether workflows are reported at all.
     *
     * @param workflowListEnabled The switch, as text
     */
    public void setWorkflowListEnabled(
        final String workflowListEnabled) {

      this.workflowListEnabled = workflowListEnabled;

    }

    /**
     * The directory the templates are loaded from.
     *
     * @return The directory
     */
    public String getTemplateLoaderPath() {

      return templateLoaderPath;

    }

    /**
     * The directory the templates are loaded from.
     *
     * @param templateLoaderPath The directory
     */
    public void setTemplateLoaderPath(
        final String templateLoaderPath) {

      this.templateLoaderPath = templateLoaderPath;

    }

    /**
     * Who answers the initiator of a report.
     *
     * @return The source of the initiator, as text
     */
    public String getInitiatorSource() {

      return initiatorSource;

    }

    /**
     * Who answers the initiator of a report.
     *
     * @param initiatorSource The source of the initiator, as text
     */
    public void setInitiatorSource(
        final String initiatorSource) {

      this.initiatorSource = initiatorSource;

    }

    /**
     * The connection to the cockpit server, and with it the REST transport.
     *
     * @return The REST section
     */
    public Rest getRest() {

      return rest;

    }

    /**
     * The connection to the cockpit server, and with it the REST transport.
     *
     * @param rest The REST section
     */
    public void setRest(
        final Rest rest) {

      this.rest = rest;

    }

    /**
     * The brokers the events are sent to, and with them the Kafka transport.
     *
     * @return The Kafka section
     */
    public Kafka getKafka() {

      return kafka;

    }

    /**
     * The brokers the events are sent to, and with them the Kafka transport.
     *
     * @param kafka The Kafka section
     */
    public void setKafka(
        final Kafka kafka) {

      this.kafka = kafka;

    }

    /**
     * What the cockpit's Process-Engine-API half reads.
     *
     * @return The Process-Engine-API section
     */
    public ProcessEngineApi getProcessEngineApi() {

      return processEngineApi;

    }

    /**
     * What the cockpit's Process-Engine-API half reads.
     *
     * @param processEngineApi The Process-Engine-API section
     */
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

    /**
     * Creates an empty section, which Spring Boot fills when it binds the properties.
     */
    public Rest() {
    }

    /**
     * The cockpit server's address, and with it the choice of this transport.
     *
     * @return The base URL of the cockpit server
     */
    public String getBaseUrl() {

      return baseUrl;

    }

    /**
     * The cockpit server's address, and with it the choice of this transport.
     *
     * @param baseUrl The base URL of the cockpit server
     */
    public void setBaseUrl(
        final String baseUrl) {

      this.baseUrl = baseUrl;

    }

    /**
     * How long establishing the connection may take.
     *
     * @return The time span, as text
     */
    public String getConnectTimeout() {

      return connectTimeout;

    }

    /**
     * How long establishing the connection may take.
     *
     * @param connectTimeout The time span, as text
     */
    public void setConnectTimeout(
        final String connectTimeout) {

      this.connectTimeout = connectTimeout;

    }

    /**
     * How long the server may take to answer.
     *
     * @return The time span, as text
     */
    public String getReadTimeout() {

      return readTimeout;

    }

    /**
     * How long the server may take to answer.
     *
     * @param readTimeout The time span, as text
     */
    public void setReadTimeout(
        final String readTimeout) {

      this.readTimeout = readTimeout;

    }

    /**
     * Whether the certificate the server presents is checked at all.
     *
     * @return The switch, as text
     */
    public String getVerifySsl() {

      return verifySsl;

    }

    /**
     * Whether the certificate the server presents is checked at all.
     *
     * @param verifySsl The switch, as text
     */
    public void setVerifySsl(
        final String verifySsl) {

      this.verifySsl = verifySsl;

    }

    /**
     * The PKCS12 file the certificate is checked against.
     *
     * @return The name of the truststore file
     */
    public String getSslTruststoreFilename() {

      return sslTruststoreFilename;

    }

    /**
     * The PKCS12 file the certificate is checked against.
     *
     * @param sslTruststoreFilename The name of the truststore file
     */
    public void setSslTruststoreFilename(
        final String sslTruststoreFilename) {

      this.sslTruststoreFilename = sslTruststoreFilename;

    }

    /**
     * The password of that file.
     *
     * @return The password of the truststore
     */
    public String getSslTruststorePassword() {

      return sslTruststorePassword;

    }

    /**
     * The password of that file.
     *
     * @param sslTruststorePassword The password of the truststore
     */
    public void setSslTruststorePassword(
        final String sslTruststorePassword) {

      this.sslTruststorePassword = sslTruststorePassword;

    }

    /**
     * The HTTP proxy the cockpit server is reached through.
     *
     * @return The proxy section
     */
    public Proxy getProxy() {

      return proxy;

    }

    /**
     * The HTTP proxy the cockpit server is reached through.
     *
     * @param proxy The proxy section
     */
    public void setProxy(
        final Proxy proxy) {

      this.proxy = proxy;

    }

    /**
     * How the workflow module identifies itself.
     *
     * @return The authentication section
     */
    public Authentication getAuthentication() {

      return authentication;

    }

    /**
     * How the workflow module identifies itself.
     *
     * @param authentication The authentication section
     */
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

    /**
     * Creates an empty section, which Spring Boot fills when it binds the properties.
     */
    public Authentication() {
    }

    /**
     * Whether a basic authentication is sent, which is version 1's switch.
     *
     * @return The switch, as text
     */
    public String getBasic() {

      return basic;

    }

    /**
     * Whether a basic authentication is sent, which is version 1's switch.
     *
     * @param basic The switch, as text
     */
    public void setBasic(
        final String basic) {

      this.basic = basic;

    }

    /**
     * The user it is sent with.
     *
     * @return The user name
     */
    public String getUsername() {

      return username;

    }

    /**
     * The user it is sent with.
     *
     * @param username The user name
     */
    public void setUsername(
        final String username) {

      this.username = username;

    }

    /**
     * The password belonging to the user.
     *
     * @return The password
     */
    public String getPassword() {

      return password;

    }

    /**
     * The password belonging to the user.
     *
     * @param password The password
     */
    public void setPassword(
        final String password) {

      this.password = password;

    }

    /**
     * The client-credentials flow the bearer token is fetched with.
     *
     * @return The OAuth section
     */
    public OAuth getOauth() {

      return oauth;

    }

    /**
     * The client-credentials flow the bearer token is fetched with.
     *
     * @param oauth The OAuth section
     */
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

    /**
     * Creates an empty section, which Spring Boot fills when it binds the properties.
     */
    public OAuth() {
    }

    /**
     * Where tokens are issued, and with it the choice of this flow.
     *
     * @return The base URL of the authorization server
     */
    public String getBaseUrl() {

      return baseUrl;

    }

    /**
     * Where tokens are issued, and with it the choice of this flow.
     *
     * @param baseUrl The base URL of the authorization server
     */
    public void setBaseUrl(
        final String baseUrl) {

      this.baseUrl = baseUrl;

    }

    /**
     * The client the token is asked for.
     *
     * @return The client id
     */
    public String getClientId() {

      return clientId;

    }

    /**
     * The client the token is asked for.
     *
     * @param clientId The client id
     */
    public void setClientId(
        final String clientId) {

      this.clientId = clientId;

    }

    /**
     * The secret belonging to it.
     *
     * @return The client secret
     */
    public String getClientSecret() {

      return clientSecret;

    }

    /**
     * The secret belonging to it.
     *
     * @param clientSecret The client secret
     */
    public void setClientSecret(
        final String clientSecret) {

      this.clientSecret = clientSecret;

    }

    /**
     * Whether the client identifies itself in an <code>Authorization</code> header.
     *
     * @return The switch, as text
     */
    public String getBasic() {

      return basic;

    }

    /**
     * Whether the client identifies itself in an <code>Authorization</code> header.
     *
     * @param basic The switch, as text
     */
    public void setBasic(
        final String basic) {

      this.basic = basic;

    }

    /**
     * How long establishing the connection may take.
     *
     * @return The time span, as text
     */
    public String getConnectTimeout() {

      return connectTimeout;

    }

    /**
     * How long establishing the connection may take.
     *
     * @param connectTimeout The time span, as text
     */
    public void setConnectTimeout(
        final String connectTimeout) {

      this.connectTimeout = connectTimeout;

    }

    /**
     * How long the authorization server may take to answer.
     *
     * @return The time span, as text
     */
    public String getReadTimeout() {

      return readTimeout;

    }

    /**
     * How long the authorization server may take to answer.
     *
     * @param readTimeout The time span, as text
     */
    public void setReadTimeout(
        final String readTimeout) {

      this.readTimeout = readTimeout;

    }

    /**
     * Whether its certificate is checked at all.
     *
     * @return The switch, as text
     */
    public String getVerifySsl() {

      return verifySsl;

    }

    /**
     * Whether its certificate is checked at all.
     *
     * @param verifySsl The switch, as text
     */
    public void setVerifySsl(
        final String verifySsl) {

      this.verifySsl = verifySsl;

    }

    /**
     * The PKCS12 file its certificate is checked against.
     *
     * @return The name of the truststore file
     */
    public String getSslTruststoreFilename() {

      return sslTruststoreFilename;

    }

    /**
     * The PKCS12 file its certificate is checked against.
     *
     * @param sslTruststoreFilename The name of the truststore file
     */
    public void setSslTruststoreFilename(
        final String sslTruststoreFilename) {

      this.sslTruststoreFilename = sslTruststoreFilename;

    }

    /**
     * The password of that file.
     *
     * @return The password of the truststore
     */
    public String getSslTruststorePassword() {

      return sslTruststorePassword;

    }

    /**
     * The password of that file.
     *
     * @param sslTruststorePassword The password of the truststore
     */
    public void setSslTruststorePassword(
        final String sslTruststorePassword) {

      this.sslTruststorePassword = sslTruststorePassword;

    }

    /**
     * The HTTP proxy the authorization server is reached through.
     *
     * @return The proxy section
     */
    public Proxy getProxy() {

      return proxy;

    }

    /**
     * The HTTP proxy the authorization server is reached through.
     *
     * @param proxy The proxy section
     */
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

    /**
     * Creates an empty section, which Spring Boot fills when it binds the properties.
     */
    public Proxy() {
    }

    /**
     * The proxy's host, which is what switches it on.
     *
     * @return The host
     */
    public String getHost() {

      return host;

    }

    /**
     * The proxy's host, which is what switches it on.
     *
     * @param host The host
     */
    public void setHost(
        final String host) {

      this.host = host;

    }

    /**
     * The proxy's port.
     *
     * @return The port, as text
     */
    public String getPort() {

      return port;

    }

    /**
     * The proxy's port.
     *
     * @param port The port, as text
     */
    public void setPort(
        final String port) {

      this.port = port;

    }

    /**
     * The user it expects, where it expects one.
     *
     * @return The user name
     */
    public String getUsername() {

      return username;

    }

    /**
     * The user it expects, where it expects one.
     *
     * @param username The user name
     */
    public void setUsername(
        final String username) {

      this.username = username;

    }

    /**
     * The password belonging to the user.
     *
     * @return The password
     */
    public String getPassword() {

      return password;

    }

    /**
     * The password belonging to the user.
     *
     * @param password The password
     */
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

    /**
     * Creates an empty section, which Spring Boot fills when it binds the properties.
     */
    public Kafka() {
    }

    /**
     * The brokers, and with them the choice of this transport.
     *
     * @return The bootstrap servers, as text
     */
    public String getBootstrapServers() {

      return bootstrapServers;

    }

    /**
     * The brokers, and with them the choice of this transport.
     *
     * @param bootstrapServers The bootstrap servers, as text
     */
    public void setBootstrapServers(
        final String bootstrapServers) {

      this.bootstrapServers = bootstrapServers;

    }

    /**
     * One topic per kind of event.
     *
     * @return The topics section
     */
    public Topics getTopics() {

      return topics;

    }

    /**
     * One topic per kind of event.
     *
     * @param topics The topics section
     */
    public void setTopics(
        final Topics topics) {

      this.topics = topics;

    }

    /**
     * Anything else the producer is to be given, for example
     * <code>kafka.properties.security.protocol</code>. A key of this map keeps the dots it was
     * written with, so a setting of the Kafka client reaches it unchanged.
     *
     * @return The other settings of the producer, by key
     */
    public Map<String, String> getProperties() {

      return properties;

    }

    /**
     * Anything else the producer is to be given, for example
     * <code>kafka.properties.security.protocol</code>. A key of this map keeps the dots it was
     * written with, so a setting of the Kafka client reaches it unchanged.
     *
     * @param properties The other settings of the producer, by key
     */
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

    /**
     * Creates an empty section, which Spring Boot fills when it binds the properties.
     */
    public Topics() {
    }

    /**
     * The topic user-task events are sent to.
     *
     * @return The name of the topic
     */
    public String getUserTask() {

      return userTask;

    }

    /**
     * The topic user-task events are sent to.
     *
     * @param userTask The name of the topic
     */
    public void setUserTask(
        final String userTask) {

      this.userTask = userTask;

    }

    /**
     * The topic workflow events are sent to.
     *
     * @return The name of the topic
     */
    public String getWorkflow() {

      return workflow;

    }

    /**
     * The topic workflow events are sent to.
     *
     * @param workflow The name of the topic
     */
    public void setWorkflow(
        final String workflow) {

      this.workflow = workflow;

    }

    /**
     * The topic a workflow module's registration is sent to.
     *
     * @return The name of the topic
     */
    public String getWorkflowModule() {

      return workflowModule;

    }

    /**
     * The topic a workflow module's registration is sent to.
     *
     * @param workflowModule The name of the topic
     */
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

    /**
     * Creates an empty section, which Spring Boot fills when it binds the properties.
     */
    public ProcessEngineApi() {
    }

    /**
     * How many delivered user tasks a node remembers.
     *
     * @return The number, as text
     */
    public String getRememberedUserTasks() {

      return rememberedUserTasks;

    }

    /**
     * How many delivered user tasks a node remembers.
     *
     * @param rememberedUserTasks The number, as text
     */
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

    /**
     * Creates an empty section, which Spring Boot fills when it binds the properties.
     */
    public WorkflowModuleOverlay() {
    }

    /**
     * What the module itself says, below <code>cockpit</code>.
     *
     * @return The module's own section
     */
    public ModuleCockpit getCockpit() {

      return cockpit;

    }

    /**
     * What the module itself says, below <code>cockpit</code>.
     *
     * @param cockpit The module's own section
     */
    public void setCockpit(
        final ModuleCockpit cockpit) {

      this.cockpit = cockpit;

    }

    /**
     * What single workflows of it say, by BPMN process id.
     *
     * @return The sections of the workflows, by BPMN process id
     */
    public Map<String, WorkflowOverlay> getWorkflows() {

      return workflows;

    }

    /**
     * What single workflows of it say, by BPMN process id.
     *
     * @param workflows The sections of the workflows, by BPMN process id
     */
    public void setWorkflows(
        final Map<String, WorkflowOverlay> workflows) {

      this.workflows = workflows;

    }


    /**
     * Turns what this workflow module said into what the neutral core reads.
     *
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
                  .getTemplatePath() == null) && (cockpit.getInitiatorSource() == null) && ((cockpit
                      .getGroupHierarchy() == null) || cockpit.getGroupHierarchy()
                          .isEmpty())) {
        return null;
      }
      return new CockpitSettings.Cockpit(
          cockpit.getWorkflowModuleUri(), cockpit.getUiUriType(), cockpit.getUiUriPath(), cockpit
              .getI18nLanguages(), cockpit
                  .getBpmnDescriptionLanguage(), cockpit.getTemplatePath(), cockpit
                      .getGroupHierarchy(), cockpit.getInitiatorSource());

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

    /** Who answers the initiator of this module's reports. */
    private String initiatorSource;

    /**
     * Creates an empty section, which Spring Boot fills when it binds the properties.
     */
    public ModuleCockpit() {
    }

    /**
     * Where the module answers the cockpit's provider APIs.
     *
     * @return The URI
     */
    public String getWorkflowModuleUri() {

      return workflowModuleUri;

    }

    /**
     * Where the module answers the cockpit's provider APIs.
     *
     * @param workflowModuleUri The URI
     */
    public void setWorkflowModuleUri(
        final String workflowModuleUri) {

      this.workflowModuleUri = workflowModuleUri;

    }

    /**
     * How the cockpit loads this module's forms.
     *
     * @return The type, as text
     */
    public String getUiUriType() {

      return uiUriType;

    }

    /**
     * How the cockpit loads this module's forms.
     *
     * @param uiUriType The type, as text
     */
    public void setUiUriType(
        final String uiUriType) {

      this.uiUriType = uiUriType;

    }

    /**
     * Where this module's forms are served from.
     *
     * @return The path
     */
    public String getUiUriPath() {

      return uiUriPath;

    }

    /**
     * Where this module's forms are served from.
     *
     * @param uiUriPath The path
     */
    public void setUiUriPath(
        final String uiUriPath) {

      this.uiUriPath = uiUriPath;

    }

    /**
     * The languages titles are reported in.
     *
     * @return The languages
     */
    public List<String> getI18nLanguages() {

      return i18nLanguages;

    }

    /**
     * The languages titles are reported in.
     *
     * @param i18nLanguages The languages
     */
    public void setI18nLanguages(
        final List<String> i18nLanguages) {

      this.i18nLanguages = i18nLanguages;

    }

    /**
     * The language the BPMN names are written in.
     *
     * @return The language
     */
    public String getBpmnDescriptionLanguage() {

      return bpmnDescriptionLanguage;

    }

    /**
     * The language the BPMN names are written in.
     *
     * @param bpmnDescriptionLanguage The language
     */
    public void setBpmnDescriptionLanguage(
        final String bpmnDescriptionLanguage) {

      this.bpmnDescriptionLanguage = bpmnDescriptionLanguage;

    }

    /**
     * The segment this module contributes to the template lookup path.
     *
     * @return The path segment
     */
    public String getTemplatePath() {

      return templatePath;

    }

    /**
     * The segment this module contributes to the template lookup path.
     *
     * @param templatePath The path segment
     */
    public void setTemplatePath(
        final String templatePath) {

      this.templatePath = templatePath;

    }

    /**
     * Which groups a group stands for.
     *
     * @return The groups each group stands for, by group
     */
    public Map<String, List<String>> getGroupHierarchy() {

      return groupHierarchy;

    }

    /**
     * Which groups a group stands for.
     *
     * @param groupHierarchy The groups each group stands for, by group
     */
    public void setGroupHierarchy(
        final Map<String, List<String>> groupHierarchy) {

      this.groupHierarchy = groupHierarchy;

    }

    /**
     * Who answers the initiator of this module's reports.
     *
     * @return The source of the initiator, as text
     */
    public String getInitiatorSource() {

      return initiatorSource;

    }

    /**
     * Who answers the initiator of this module's reports.
     *
     * @param initiatorSource The source of the initiator, as text
     */
    public void setInitiatorSource(
        final String initiatorSource) {

      this.initiatorSource = initiatorSource;

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

    /**
     * Creates an empty section, which Spring Boot fills when it binds the properties.
     */
    public WorkflowOverlay() {
    }

    /**
     * What this workflow says, below <code>cockpit</code>.
     *
     * @return The workflow's own section
     */
    public WorkflowCockpit getCockpit() {

      return cockpit;

    }

    /**
     * What this workflow says, below <code>cockpit</code>.
     *
     * @param cockpit The workflow's own section
     */
    public void setCockpit(
        final WorkflowCockpit cockpit) {

      this.cockpit = cockpit;

    }

    /**
     * What single user tasks of it say, by task definition.
     *
     * @return The sections of the user tasks, by task definition
     */
    public Map<String, UserTaskOverlay> getUserTasks() {

      return userTasks;

    }

    /**
     * What single user tasks of it say, by task definition.
     *
     * @param userTasks The sections of the user tasks, by task definition
     */
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
                  : cockpit.getTemplatePath(), ofTheUserTasks, cockpit == null
                      ? null
                      : cockpit.getInitiatorSource());

    }

    private boolean saysNothing() {

      return (cockpit == null) || ((cockpit.getI18nLanguages() == null) && (cockpit
          .getBpmnDescriptionLanguage() == null) && (cockpit.getTemplatePath() == null) && (cockpit
              .getInitiatorSource() == null));

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

    /** Who answers the initiator of this workflow's reports. */
    private String initiatorSource;

    /**
     * Creates an empty section, which Spring Boot fills when it binds the properties.
     */
    public WorkflowCockpit() {
    }

    /**
     * The languages this workflow's titles are reported in.
     *
     * @return The languages
     */
    public List<String> getI18nLanguages() {

      return i18nLanguages;

    }

    /**
     * The languages this workflow's titles are reported in.
     *
     * @param i18nLanguages The languages
     */
    public void setI18nLanguages(
        final List<String> i18nLanguages) {

      this.i18nLanguages = i18nLanguages;

    }

    /**
     * The language this workflow's BPMN names are written in.
     *
     * @return The language
     */
    public String getBpmnDescriptionLanguage() {

      return bpmnDescriptionLanguage;

    }

    /**
     * The language this workflow's BPMN names are written in.
     *
     * @param bpmnDescriptionLanguage The language
     */
    public void setBpmnDescriptionLanguage(
        final String bpmnDescriptionLanguage) {

      this.bpmnDescriptionLanguage = bpmnDescriptionLanguage;

    }

    /**
     * The segment this workflow contributes to the template lookup path.
     *
     * @return The path segment
     */
    public String getTemplatePath() {

      return templatePath;

    }

    /**
     * The segment this workflow contributes to the template lookup path.
     *
     * @param templatePath The path segment
     */
    public void setTemplatePath(
        final String templatePath) {

      this.templatePath = templatePath;

    }

    /**
     * Who answers the initiator of this workflow's reports.
     *
     * @return The source of the initiator, as text
     */
    public String getInitiatorSource() {

      return initiatorSource;

    }

    /**
     * Who answers the initiator of this workflow's reports.
     *
     * @param initiatorSource The source of the initiator, as text
     */
    public void setInitiatorSource(
        final String initiatorSource) {

      this.initiatorSource = initiatorSource;

    }

  }

  /**
   * The cockpit's view of one user task of a workflow.
   */
  public static class UserTaskOverlay {

    /** What this user task says, below <code>cockpit</code>. */
    private UserTaskCockpit cockpit;

    /**
     * Creates an empty section, which Spring Boot fills when it binds the properties.
     */
    public UserTaskOverlay() {
    }

    /**
     * What this user task says, below <code>cockpit</code>.
     *
     * @return The user task's own section
     */
    public UserTaskCockpit getCockpit() {

      return cockpit;

    }

    /**
     * What this user task says, below <code>cockpit</code>.
     *
     * @param cockpit The user task's own section
     */
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

    /**
     * Creates an empty section, which Spring Boot fills when it binds the properties.
     */
    public UserTaskCockpit() {
    }

    /**
     * The segment this user task contributes to the template lookup path.
     *
     * @return The path segment
     */
    public String getTemplatePath() {

      return templatePath;

    }

    /**
     * The segment this user task contributes to the template lookup path.
     *
     * @param templatePath The path segment
     */
    public void setTemplatePath(
        final String templatePath) {

      this.templatePath = templatePath;

    }

  }

}
