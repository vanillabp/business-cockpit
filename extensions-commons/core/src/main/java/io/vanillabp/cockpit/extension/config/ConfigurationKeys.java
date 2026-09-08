package io.vanillabp.cockpit.extension.config;

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

}
