package io.vanillabp.cockpit.extension.config;

import java.util.List;
import java.util.Map;

/**
 * The Business Cockpit's configuration tree as an application wrote it, in the shape version 1
 * of the cockpit had: <code>vanillabp.cockpit</code> for what holds for the whole application,
 * <code>vanillabp.workflow-modules.&lt;id&gt;.cockpit</code> for one workflow module, and the
 * two levels below it for one workflow and one of its user tasks.
 * <p>
 * Every leaf is the text the application wrote, not a parsed value. The two platforms bind
 * this tree with the means they have - a JavaBean overlay on Spring Boot, a configuration
 * mapping on Quarkus - and both hand over this one object, so that a value which is not a
 * number, not a span of time or not a boolean is answered by the same message on both.
 * {@link BusinessCockpitConfiguration#readAndValidate} is where that happens, and it is the
 * only place which knows what a key means.
 *
 * @param userTasksEnabled Whether user tasks are reported at all, <code>true</code> where
 *          nothing was configured
 * @param workflowListEnabled Whether workflows are reported at all, <code>true</code> where
 *          nothing was configured
 * @param templateLoaderPath The directory Freemarker loads the templates from
 * @param rest The REST transport, or <code>null</code> where nothing below <code>rest</code>
 *          was written - a section which says nothing is a section which is not there, on either
 *          platform
 * @param kafka The Kafka transport, or <code>null</code>
 * @param processEngineApi What the Process-Engine-API half of the cockpit reads, or
 *          <code>null</code>
 * @param workflowModules What single workflow modules wrote, by workflow module id
 */
public record CockpitSettings(
                              String userTasksEnabled,
                              String workflowListEnabled,
                              String templateLoaderPath,
                              Rest rest,
                              Kafka kafka,
                              ProcessEngineApi processEngineApi,
                              Map<String, WorkflowModule> workflowModules) {

  public CockpitSettings {
    workflowModules = workflowModules == null ? Map.of() : Map.copyOf(workflowModules);
  }

  /**
   * @return An application which wrote nothing at all about the Business Cockpit
   */
  public static CockpitSettings none() {

    return new CockpitSettings(null, null, null, null, null, null, Map.of());

  }

  /**
   * What one workflow module wrote, which is nothing where it takes no part in the cockpit.
   *
   * @param workflowModuleId The module
   * @return Its section, or <code>null</code>
   */
  public WorkflowModule workflowModule(
      final String workflowModuleId) {

    return workflowModules.get(workflowModuleId);

  }

  /**
   * Everything below <code>vanillabp.cockpit.rest</code>.
   *
   * @param baseUrl The cockpit server's address, and with it the choice of this transport
   * @param connectTimeout How long establishing the connection may take
   * @param readTimeout How long the server may take to answer
   * @param verifySsl Whether the certificate the server presents is checked at all
   * @param sslTruststoreFilename The PKCS12 file the certificate is checked against
   * @param sslTruststorePassword The password of that file
   * @param proxy The HTTP proxy the server is reached through
   * @param authentication How the workflow module identifies itself
   */
  public record Rest(
                     String baseUrl,
                     String connectTimeout,
                     String readTimeout,
                     String verifySsl,
                     String sslTruststoreFilename,
                     String sslTruststorePassword,
                     Proxy proxy,
                     Authentication authentication) {
  }

  /**
   * An HTTP proxy, below <code>rest</code> and below the token client of the OAuth flow.
   *
   * @param host The proxy's host, which is what switches it on
   * @param port The proxy's port
   * @param username The user it expects, where it expects one
   * @param password The password belonging to the user
   */
  public record Proxy(
                      String host,
                      String port,
                      String username,
                      String password) {
  }

  /**
   * Everything below <code>rest.authentication</code>.
   *
   * @param basic Whether the workflow module sends a basic authentication, as in version 1
   * @param username The user it sends
   * @param password The password belonging to the user
   * @param oauth The client-credentials flow, where the cockpit server expects a token
   */
  public record Authentication(
                               String basic,
                               String username,
                               String password,
                               OAuth oauth) {
  }

  /**
   * Everything below <code>rest.authentication.oauth</code>.
   * <p>
   * In version 1 this section was a client configuration of its own, so an authorization
   * server behind another proxy or with another certificate was reachable. It stays one: the
   * token client inherits nothing from <code>rest</code>, and what is not written here is the
   * default rather than what the cockpit server's client uses.
   *
   * @param baseUrl Where tokens are issued, and with it the choice of this flow
   * @param clientId The client the token is asked for
   * @param clientSecret The secret belonging to it
   * @param basic Whether the client identifies itself in an <code>Authorization</code> header
   * @param connectTimeout How long establishing the connection to the authorization server may
   *          take
   * @param readTimeout How long the authorization server may take to answer
   * @param verifySsl Whether its certificate is checked at all
   * @param sslTruststoreFilename The PKCS12 file its certificate is checked against
   * @param sslTruststorePassword The password of that file
   * @param proxy The HTTP proxy the authorization server is reached through
   */
  public record OAuth(
                      String baseUrl,
                      String clientId,
                      String clientSecret,
                      String basic,
                      String connectTimeout,
                      String readTimeout,
                      String verifySsl,
                      String sslTruststoreFilename,
                      String sslTruststorePassword,
                      Proxy proxy) {
  }

  /**
   * Everything below <code>vanillabp.cockpit.kafka</code>.
   *
   * @param bootstrapServers The brokers, and with them the choice of this transport
   * @param topics One topic per kind of event
   * @param properties Anything else the producer is to be given
   */
  public record Kafka(
                      String bootstrapServers,
                      Topics topics,
                      Map<String, String> properties) {

    public Kafka {
      properties = properties == null ? Map.of() : Map.copyOf(properties);
    }

  }

  /**
   * The three topics the cockpit server reads.
   *
   * @param userTask The topic user-task events are sent to
   * @param workflow The topic workflow events are sent to
   * @param workflowModule The topic a workflow module's registration is sent to
   */
  public record Topics(
                       String userTask,
                       String workflow,
                       String workflowModule) {
  }

  /**
   * The one setting of the cockpit's Process-Engine-API half, which is read by that half and
   * declared here because a key which is not declared fails the boot on Quarkus.
   *
   * @param rememberedUserTasks How many delivered user tasks a node remembers
   */
  public record ProcessEngineApi(
                                 String rememberedUserTasks) {
  }

  /**
   * What one workflow module wrote: its own <code>cockpit</code> section, and what single
   * workflows of it wrote below <code>workflows</code>.
   *
   * @param cockpit The module's own section, or <code>null</code> where it wrote none
   * @param workflows What single workflows wrote, by BPMN process id
   */
  public record WorkflowModule(
                               Cockpit cockpit,
                               Map<String, Workflow> workflows) {

    public WorkflowModule {
      workflows = workflows == null ? Map.of() : Map.copyOf(workflows);
    }

    /**
     * Whether the module took part in the Business Cockpit at all.
     * <p>
     * A module which wrote nothing reports nothing, which is an application using the cockpit
     * for some of its modules and not for others. A module which wrote something incomplete is
     * a defect and is reported as one.
     *
     * @return Whether it wrote anything
     */
    public boolean saysNothing() {

      return (cockpit == null) && workflows.isEmpty();

    }

  }

  /**
   * Everything below <code>vanillabp.workflow-modules.&lt;id&gt;.cockpit</code>.
   *
   * @param workflowModuleUri Where the module answers the cockpit's provider APIs
   * @param uiUriType How the cockpit loads this module's forms
   * @param uiUriPath Where this module's forms are served from
   * @param i18nLanguages The languages titles are reported in
   * @param bpmnDescriptionLanguage The language the names in the BPMN files are written in
   * @param templatePath The segment this module contributes to the template lookup path
   * @param groupHierarchy Which groups a group stands for
   */
  public record Cockpit(
                        String workflowModuleUri,
                        String uiUriType,
                        String uiUriPath,
                        List<String> i18nLanguages,
                        String bpmnDescriptionLanguage,
                        String templatePath,
                        Map<String, List<String>> groupHierarchy) {

    public Cockpit {
      i18nLanguages = i18nLanguages == null ? null : List.copyOf(i18nLanguages);
      groupHierarchy = groupHierarchy == null ? Map.of() : Map.copyOf(groupHierarchy);
    }

  }

  /**
   * Everything below
   * <code>vanillabp.workflow-modules.&lt;id&gt;.workflows.&lt;process&gt;.cockpit</code>, and
   * what the user tasks of that workflow wrote.
   *
   * @param i18nLanguages The languages this workflow's titles are reported in, or
   *          <code>null</code> where the module decides
   * @param bpmnDescriptionLanguage The language this workflow's BPMN names are written in, or
   *          <code>null</code>
   * @param templatePath The segment this workflow contributes to the template lookup path, or
   *          <code>null</code>
   * @param userTasks What single user tasks of this workflow wrote, by task definition
   */
  public record Workflow(
                         List<String> i18nLanguages,
                         String bpmnDescriptionLanguage,
                         String templatePath,
                         Map<String, UserTask> userTasks) {

    public Workflow {
      i18nLanguages = i18nLanguages == null ? null : List.copyOf(i18nLanguages);
      userTasks = userTasks == null ? Map.of() : Map.copyOf(userTasks);
    }

  }

  /**
   * Everything below
   * <code>…workflows.&lt;process&gt;.user-tasks.&lt;task&gt;.cockpit</code>.
   *
   * @param templatePath The segment this user task contributes to the template lookup path
   */
  public record UserTask(
                         String templatePath) {
  }

}
