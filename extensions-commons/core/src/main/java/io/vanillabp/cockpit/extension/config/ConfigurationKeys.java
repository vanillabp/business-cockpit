package io.vanillabp.cockpit.extension.config;

import java.util.List;
import java.util.stream.Stream;

/**
 * The property keys the Business Cockpit extension owns, spelled once so that a message can
 * name the key a developer has to add.
 * <p>
 * They are the keys version 1 of the Business Cockpit read, in the places it read them:
 *
 * <pre>
 * vanillabp.cockpit.&lt;key&gt;
 * vanillabp.workflow-modules.&lt;moduleId&gt;.cockpit.&lt;key&gt;
 * vanillabp.workflow-modules.&lt;moduleId&gt;.workflows.&lt;process&gt;.cockpit.&lt;key&gt;
 * vanillabp.workflow-modules.&lt;moduleId&gt;.workflows.&lt;process&gt;.user-tasks.&lt;task&gt;.cockpit.&lt;key&gt;
 * </pre>
 *
 * An application upgrades to version 2 by changing a dependency, and the Business Cockpit
 * having become an extension of the VanillaBP platform is nothing it has to know about. What
 * did change is written down in the decision log and in the migration pages, one reason per
 * key.
 * <p>
 * The levels are asked most specific first, per key: a user task beats its workflow, a
 * workflow beats its workflow module. What holds for the whole application - the transport,
 * the templates, the two switches - is written once at the top and exists nowhere else.
 */
public final class ConfigurationKeys {

  /** The id naming this extension where VanillaBP collects what an extension contributes. */
  public static final String EXTENSION_ID = "business-cockpit";

  /** Where the settings of the whole application live. */
  public static final String GLOBAL_PREFIX = "vanillabp.cockpit";

  /** The section of a workflow module, of one of its workflows, and of a user task. */
  public static final String COCKPIT_SECTION = "cockpit";

  /** Whether user tasks are reported to the cockpit at all. */
  public static final String USER_TASKS_ENABLED = "user-tasks-enabled";

  /** Whether workflows are reported to the cockpit at all. */
  public static final String WORKFLOW_LIST_ENABLED = "workflow-list-enabled";

  /** The address of the cockpit server's BPMS API, and with it the choice of the REST transport. */
  public static final String REST_BASE_URL = "rest.base-url";

  /** Whether the workflow module identifies itself with a basic authentication. */
  public static final String REST_BASIC = "rest.authentication.basic";

  /** The user of that basic authentication. */
  public static final String REST_USERNAME = "rest.authentication.username";

  /** The password belonging to {@link #REST_USERNAME}. */
  public static final String REST_PASSWORD = "rest.authentication.password";

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

  /** The languages titles are reported in, written as a list. */
  public static final String I18N_LANGUAGES = "i18n-languages";

  /** The language the names written in the BPMN are in. */
  public static final String BPMN_DESCRIPTION_LANGUAGE = "bpmn-description-language";

  /**
   * The group hierarchy: one entry per group, its value being the groups it stands for, written
   * as version 1 wrote it.
   *
   * <pre>
   * group-hierarchy:
   *   TEAM_LEAD:
   *     - TEAM_MEMBER
   *     - ASSISTANT
   * </pre>
   */
  public static final String GROUP_HIERARCHY = "group-hierarchy";

  /** The segment this level contributes to the template lookup path. */
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

  /** Where the connection to the cockpit server is configured. */
  public static final String REST_PREFIX = "rest.";

  /** Where the connection to the authorization server is configured, as in version 1. */
  public static final String REST_OAUTH_PREFIX = "rest.authentication.oauth.";

  /** How long establishing a connection may take, below one of the two connection sections. */
  public static final String CONNECT_TIMEOUT = "connect-timeout";

  /** How long a server may take to answer, below one of the two connection sections. */
  public static final String READ_TIMEOUT = "read-timeout";

  /** Whether the certificate a server presents is checked at all. */
  public static final String VERIFY_SSL = "verify-ssl";

  /** The PKCS12 file holding the certificates a server's is checked against. */
  public static final String SSL_TRUSTSTORE_FILENAME = "ssl-truststore-filename";

  /** The password of {@link #SSL_TRUSTSTORE_FILENAME}. */
  public static final String SSL_TRUSTSTORE_PASSWORD = "ssl-truststore-password";

  /** The host of the HTTP proxy a server is reached through, which is what switches it on. */
  public static final String PROXY_HOST = "proxy.host";

  /** The port of that HTTP proxy. */
  public static final String PROXY_PORT = "proxy.port";

  /** The user that HTTP proxy expects, where it expects one. */
  public static final String PROXY_USERNAME = "proxy.username";

  /** The password belonging to {@link #PROXY_USERNAME}. */
  public static final String PROXY_PASSWORD = "proxy.password";

  /** How long a connection to the cockpit server may take to establish. */
  public static final String REST_CONNECT_TIMEOUT = REST_PREFIX + CONNECT_TIMEOUT;

  /** How long the cockpit server may take to answer. */
  public static final String REST_READ_TIMEOUT = REST_PREFIX + READ_TIMEOUT;

  /** Whether the certificate the cockpit server presents is checked at all. */
  public static final String REST_VERIFY_SSL = REST_PREFIX + VERIFY_SSL;

  /** The PKCS12 file holding the certificates the cockpit server's is checked against. */
  public static final String REST_SSL_TRUSTSTORE_FILENAME = REST_PREFIX + SSL_TRUSTSTORE_FILENAME;

  /** The password of {@link #REST_SSL_TRUSTSTORE_FILENAME}. */
  public static final String REST_SSL_TRUSTSTORE_PASSWORD = REST_PREFIX + SSL_TRUSTSTORE_PASSWORD;

  /** How many delivered user tasks the Process-Engine-API half of the cockpit remembers. */
  public static final String REMEMBERED_USER_TASKS = "process-engine-api.remembered-user-tasks";

  /**
   * What both connections are configured with: the one to the cockpit server, below
   * <code>rest</code>, and the one to the authorization server, below the OAuth flow.
   */
  public static final List<String> CONNECTION_KEYS = List
      .of(
          CONNECT_TIMEOUT, READ_TIMEOUT, VERIFY_SSL, SSL_TRUSTSTORE_FILENAME,
          SSL_TRUSTSTORE_PASSWORD, PROXY_HOST, PROXY_PORT, PROXY_USERNAME, PROXY_PASSWORD);

  /**
   * Every key which stands below <code>vanillabp.cockpit</code>, which is what tells a
   * misspelled key from one this extension reads.
   */
  public static final List<String> GLOBAL_KEYS = Stream
      .of(
          Stream
              .of(
                  USER_TASKS_ENABLED, WORKFLOW_LIST_ENABLED, TEMPLATE_LOADER_PATH, REST_BASE_URL,
                  REST_BASIC, REST_USERNAME, REST_PASSWORD, REST_OAUTH_BASE_URL,
                  REST_OAUTH_CLIENT_ID, REST_OAUTH_CLIENT_SECRET, REST_OAUTH_BASIC,
                  KAFKA_BOOTSTRAP_SERVERS, KAFKA_TOPIC_USER_TASK, KAFKA_TOPIC_WORKFLOW,
                  KAFKA_TOPIC_WORKFLOW_MODULE, REMEMBERED_USER_TASKS),
          CONNECTION_KEYS.stream().map(REST_PREFIX::concat),
          CONNECTION_KEYS.stream().map(REST_OAUTH_PREFIX::concat))
      .flatMap(keys -> keys)
      .toList();

  /**
   * The keys which stand below a whole workflow module. Whatever a workflow module says about
   * the cockpit, it says with one of these.
   */
  public static final List<String> KEYS_OF_A_WORKFLOW_MODULE = List
      .of(
          WORKFLOW_MODULE_URI, UI_URI_TYPE, UI_URI_PATH, I18N_LANGUAGES,
          BPMN_DESCRIPTION_LANGUAGE, TEMPLATE_PATH, GROUP_HIERARCHY);

  /**
   * The keys the extension reads below a single workflow, which are the ones a workflow may
   * differ from its module in.
   */
  public static final List<String> KEYS_OF_A_WORKFLOW = List
      .of(BPMN_DESCRIPTION_LANGUAGE, I18N_LANGUAGES, TEMPLATE_PATH);

  /** The keys the extension reads below a single user task. */
  public static final List<String> KEYS_OF_A_USER_TASK = List.of(TEMPLATE_PATH);

  /** Where the settings of single workflows of a workflow module stand. */
  public static final String WORKFLOWS_SECTION = "workflows";

  /** Where the settings of single user tasks of a workflow stand. */
  public static final String USER_TASKS_SECTION = "user-tasks";

  private ConfigurationKeys() {
  }

  /**
   * The full property key of a setting of the whole application, for a message which asks
   * somebody to add it.
   *
   * @param key One of the keys above
   * @return e.g. <code>vanillabp.cockpit.rest.base-url</code>
   */
  public static String globalKey(
      final String key) {

    return key.isEmpty()
        ? GLOBAL_PREFIX
        : "%s.%s".formatted(GLOBAL_PREFIX, key);

  }

  /**
   * The full property key of a workflow module's setting.
   *
   * @param workflowModuleId The workflow module
   * @param key One of the keys above
   * @return e.g. <code>vanillabp.workflow-modules.taxi-ride.cockpit.ui-uri-path</code>
   */
  public static String workflowModuleKey(
      final String workflowModuleId,
      final String key) {

    return key.isEmpty()
        ? "vanillabp.workflow-modules.%s.%s".formatted(workflowModuleId, COCKPIT_SECTION)
        : "vanillabp.workflow-modules.%s.%s.%s"
            .formatted(workflowModuleId, COCKPIT_SECTION, key);

  }

  /**
   * The full property key of a setting of one workflow of a workflow module.
   *
   * @param workflowModuleId The workflow module
   * @param bpmnProcessId The workflow
   * @param key One of {@link #KEYS_OF_A_WORKFLOW}
   * @return e.g.
   *         <code>vanillabp.workflow-modules.taxi-ride.workflows.TaxiRide.cockpit.template-path</code>
   */
  public static String workflowKey(
      final String workflowModuleId,
      final String bpmnProcessId,
      final String key) {

    return "vanillabp.workflow-modules.%s.%s.%s.%s.%s"
        .formatted(
            workflowModuleId, WORKFLOWS_SECTION, bpmnProcessId, COCKPIT_SECTION, key);

  }

}
