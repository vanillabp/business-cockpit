package io.vanillabp.cockpit.extension.config;

import java.util.List;

/**
 * The property keys the Business Cockpit extension owns, spelled once so that a message can
 * name the key a developer has to add.
 * <p>
 * All of them live below the two locations the VanillaBP core reserves for an extension:
 *
 * <pre>
 * vanillabp.extensions.business-cockpit.&lt;key&gt;
 * vanillabp.workflow-modules.&lt;moduleId&gt;.extensions.business-cockpit.&lt;key&gt;
 * </pre>
 *
 * A workflow module's value wins per key, and it keeps whatever the global section says about
 * the rest - so a setting which is the same everywhere is written once at the top.
 * <p>
 * Two of the keys mean something for a single workflow and one of them for a single user task.
 * Those two levels stand INSIDE the module's own section, below <code>workflows</code>:
 *
 * <pre>
 * vanillabp.workflow-modules.&lt;moduleId&gt;.extensions.business-cockpit.workflows.&lt;process&gt;.&lt;key&gt;
 * vanillabp.workflow-modules.&lt;moduleId&gt;.extensions.business-cockpit.workflows.&lt;process&gt;.user-tasks.&lt;task&gt;.template-path
 * </pre>
 *
 * The core reserves two locations for an extension and says that deeper levels are the
 * extension's own business, which is what these are: everything below the extension's id is
 * handed over as written, so both platforms carry them without knowing what they mean.
 */
public final class ConfigurationKeys {

  /** The id naming this extension in the configuration and in every message about it. */
  public static final String EXTENSION_ID = "business-cockpit";

  /** Where the extension's global settings live. */
  public static final String GLOBAL_PREFIX = "vanillabp.extensions."
      + EXTENSION_ID;

  /** The address of the cockpit server's BPMS API, and with it the choice of the REST transport. */
  public static final String REST_BASE_URL = "rest.base-url";

  /** The user of the basic authentication the cockpit server expects, where it expects one. */
  public static final String REST_USERNAME = "rest.username";

  /** The password belonging to {@link #REST_USERNAME}. */
  public static final String REST_PASSWORD = "rest.password";

  /** The brokers of the Kafka transport, and with it the choice of that transport. */
  public static final String KAFKA_BOOTSTRAP_SERVERS = "kafka.bootstrap-servers";

  /** The topic user-task events are sent to. */
  public static final String KAFKA_TOPIC_USER_TASK = "kafka.topics.user-task";

  /** The topic workflow events are sent to. */
  public static final String KAFKA_TOPIC_WORKFLOW = "kafka.topics.workflow";

  /** The topic the registration of a workflow module is sent to. */
  public static final String KAFKA_TOPIC_WORKFLOW_MODULE = "kafka.topics.workflow-module";

  /**
   * The prefix of arbitrary Kafka producer settings handed to the client unchanged, e.g.
   * <code>kafka.properties.security.protocol</code>.
   */
  public static final String KAFKA_PROPERTIES_PREFIX = "kafka.properties.";

  /** The directory Freemarker loads the extension's templates from. */
  public static final String TEMPLATE_LOADER_PATH = "template-loader-path";

  /** Where the workflow module answers the cockpit's provider APIs. */
  public static final String WORKFLOW_MODULE_URI = "workflow-module-uri";

  /** How the cockpit is to render this module's forms: EXTERNAL or WEBPACK_MF_REACT. */
  public static final String UI_URI_TYPE = "ui-uri-type";

  /** Where this module's forms are served from. */
  public static final String UI_URI_PATH = "ui-uri-path";

  /** The languages titles are reported in, comma separated. */
  public static final String I18N_LANGUAGES = "i18n-languages";

  /** The language the names written in the BPMN are in. */
  public static final String BPMN_DESCRIPTION_LANGUAGE = "bpmn-description-language";

  /**
   * The prefix of the group hierarchy, one key per group, the value being its target groups
   * comma separated, e.g. <code>group-hierarchy.TEAM_LEAD=TEAM_MEMBER,ASSISTANT</code>.
   */
  public static final String GROUP_HIERARCHY_PREFIX = "group-hierarchy.";

  /** The segment this module contributes to the template lookup path. */
  public static final String TEMPLATE_PATH = "template-path";

  /** The address of the token endpoint of the client-credentials flow, and with it its choice. */
  public static final String REST_OAUTH_BASE_URL = "rest.authentication.oauth.base-url";

  /** The client the token is asked for. */
  public static final String REST_OAUTH_CLIENT_ID = "rest.authentication.oauth.client-id";

  /** The secret belonging to {@link #REST_OAUTH_CLIENT_ID}. */
  public static final String REST_OAUTH_CLIENT_SECRET = "rest.authentication.oauth.client-secret";

  /**
   * Whether the client identifies itself in an <code>Authorization</code> header rather than in
   * the form the token request carries.
   */
  public static final String REST_OAUTH_BASIC = "rest.authentication.oauth.basic";

  /** How long a connection to the cockpit server may take to establish. */
  public static final String REST_CONNECT_TIMEOUT = "rest.connect-timeout";

  /** How long the cockpit server may take to answer. */
  public static final String REST_READ_TIMEOUT = "rest.read-timeout";

  /** The host of the HTTP proxy the cockpit server is reached through. */
  public static final String REST_PROXY_HOST = "rest.proxy.host";

  /** The port of the HTTP proxy. */
  public static final String REST_PROXY_PORT = "rest.proxy.port";

  /** The user the HTTP proxy expects, where it expects one. */
  public static final String REST_PROXY_USERNAME = "rest.proxy.username";

  /** The password belonging to {@link #REST_PROXY_USERNAME}. */
  public static final String REST_PROXY_PASSWORD = "rest.proxy.password";

  /** Whether the certificate the cockpit server presents is checked at all. */
  public static final String REST_VERIFY_SSL = "rest.verify-ssl";

  /** The PKCS12 file holding the certificates the cockpit server's is checked against. */
  public static final String REST_SSL_TRUSTSTORE_FILENAME = "rest.ssl-truststore-filename";

  /** The password of {@link #REST_SSL_TRUSTSTORE_FILENAME}. */
  public static final String REST_SSL_TRUSTSTORE_PASSWORD = "rest.ssl-truststore-password";

  /**
   * The keys the extension reads below a single workflow, which are the ones a workflow may
   * differ from its module in.
   */
  public static final List<String> KEYS_OF_A_WORKFLOW = List
      .of(BPMN_DESCRIPTION_LANGUAGE, I18N_LANGUAGES, TEMPLATE_PATH);

  /** The keys the extension reads below a single user task. */
  public static final List<String> KEYS_OF_A_USER_TASK = List.of(TEMPLATE_PATH);

  /**
   * The prefix of everything one single workflow says, e.g.
   * <code>workflows.TaxiRide.template-path</code>.
   */
  public static final String WORKFLOWS_PREFIX = "workflows.";

  /**
   * What stands between a workflow and one of its user tasks, e.g.
   * <code>workflows.TaxiRide.user-tasks.approve.template-path</code>.
   */
  public static final String USER_TASKS_INFIX = ".user-tasks.";

  private ConfigurationKeys() {
  }

  /**
   * The full property key of a global setting, for a message which asks somebody to add it.
   *
   * @param key One of the keys above
   * @return e.g. <code>vanillabp.extensions.business-cockpit.rest.base-url</code>
   */
  public static String globalKey(
      final String key) {

    return "%s.%s".formatted(GLOBAL_PREFIX, key);

  }

  /**
   * The full property key of a workflow module's setting.
   *
   * @param workflowModuleId The workflow module
   * @param key One of the keys above
   * @return e.g.
   *         <code>vanillabp.workflow-modules.taxi-ride.extensions.business-cockpit.ui-uri-path</code>
   */
  public static String workflowModuleKey(
      final String workflowModuleId,
      final String key) {

    return "vanillabp.workflow-modules.%s.extensions.%s.%s".formatted(
        workflowModuleId, EXTENSION_ID, key);

  }

  /**
   * What one workflow of a workflow module says, below that module's own section.
   *
   * @param bpmnProcessId The workflow
   * @param key One of {@link #KEYS_OF_A_WORKFLOW}
   * @return e.g. <code>workflows.TaxiRide.template-path</code>
   */
  public static String ofWorkflow(
      final String bpmnProcessId,
      final String key) {

    return "%s%s.%s".formatted(WORKFLOWS_PREFIX, bpmnProcessId, key);

  }

  /**
   * What one user task of one workflow says, below that module's own section.
   *
   * @param bpmnProcessId The workflow
   * @param taskDefinition The user task
   * @param key One of {@link #KEYS_OF_A_USER_TASK}
   * @return e.g. <code>workflows.TaxiRide.user-tasks.approve.template-path</code>
   */
  public static String ofUserTask(
      final String bpmnProcessId,
      final String taskDefinition,
      final String key) {

    return "%s%s%s%s.%s".formatted(
        WORKFLOWS_PREFIX, bpmnProcessId, USER_TASKS_INFIX, taskDefinition, key);

  }

  /**
   * The full property key of a setting of one workflow of a workflow module.
   *
   * @param workflowModuleId The workflow module
   * @param bpmnProcessId The workflow
   * @param key One of {@link #KEYS_OF_A_WORKFLOW}
   * @return e.g.
   *         <code>vanillabp.workflow-modules.taxi-ride.extensions.business-cockpit.workflows.TaxiRide.template-path</code>
   */
  public static String workflowKey(
      final String workflowModuleId,
      final String bpmnProcessId,
      final String key) {

    return workflowModuleKey(workflowModuleId, ofWorkflow(bpmnProcessId, key));

  }

  /**
   * The full property key of a setting of one user task of one workflow.
   *
   * @param workflowModuleId The workflow module
   * @param bpmnProcessId The workflow
   * @param taskDefinition The user task
   * @param key One of {@link #KEYS_OF_A_USER_TASK}
   * @return e.g.
   *         <code>vanillabp.workflow-modules.taxi-ride.extensions.business-cockpit.workflows.TaxiRide.user-tasks.approve.template-path</code>
   */
  public static String userTaskKey(
      final String workflowModuleId,
      final String bpmnProcessId,
      final String taskDefinition,
      final String key) {

    return workflowModuleKey(
        workflowModuleId, ofUserTask(bpmnProcessId, taskDefinition, key));

  }

}
