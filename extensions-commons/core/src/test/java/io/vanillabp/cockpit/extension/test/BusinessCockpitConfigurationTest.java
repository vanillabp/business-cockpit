package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.config.BusinessCockpitConfiguration;
import io.vanillabp.cockpit.extension.config.UiUriType;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What an application is told about its configuration, and when.
 * <p>
 * Every defect is reported while the application starts, all of them in one message, and every
 * line names the property key which fixes it - a developer reaching a working setup from the
 * log rather than from the documentation is what these assertions are about.
 */
@ExtendWith(SuppressOutputExtension.class)
public class BusinessCockpitConfigurationTest {

  private static BusinessCockpitConfiguration read(
      final ConfigurationFixture fixture,
      final boolean templatingAvailable) {

    return BusinessCockpitConfiguration.readAndValidate(fixture.build(), templatingAvailable);

  }

  private static String defectsOf(
      final ConfigurationFixture fixture,
      final boolean templatingAvailable) {

    return assertThrows(
        IllegalStateException.class,
        () -> read(fixture, templatingAvailable)).getMessage();

  }

  @Test
  @DisplayName("A complete REST configuration is read")
  public void restIsRead() {

    final var configuration = read(ConfigurationFixture.aConfiguredApplication(), false);

    assertNotNull(configuration.getRest());
    assertEquals("http://localhost:8080", configuration.getRest().baseUrl());
    assertNull(configuration.getKafka());
    final var module = configuration.workflowModule(ConfigurationFixture.WORKFLOW_MODULE);
    assertEquals("http://localhost:8081", module.workflowModuleUri());
    assertEquals(UiUriType.WEBPACK_MF_REACT, module.uiUriType());
    assertEquals(List.of("en", "de"), module.i18nLanguages());
    assertEquals("en", module.bpmnDescriptionLanguage());
    assertEquals(ConfigurationFixture.WORKFLOW_MODULE, module.templatePath());

  }

  @Test
  @DisplayName("A complete Kafka configuration is read, including free-form producer settings")
  public void kafkaIsRead() {

    final var configuration = read(
        ConfigurationFixture
            .aConfiguredApplication()
            .without("rest.base-url")
            .with("kafka.bootstrap-servers", "broker:9092")
            .with("kafka.topics.user-task", "user-task")
            .with("kafka.topics.workflow", "workflow")
            .with("kafka.topics.workflow-module", "workflow-module")
            .with("kafka.properties.security.protocol", "SSL"),
        false);

    assertNull(configuration.getRest());
    assertNotNull(configuration.getKafka());
    assertEquals("broker:9092", configuration.getKafka().bootstrapServers());
    assertEquals("user-task", configuration.getKafka().userTaskTopic());
    assertEquals(
        "SSL", configuration.getKafka().producerProperties().get("security.protocol"));

  }

  @Test
  @DisplayName("Without a transport the application is told about both of them")
  public void neitherTransportNamesBoth() {

    final var defects = defectsOf(
        ConfigurationFixture.aConfiguredApplication().without("rest.base-url"), false);

    assertTrue(
        defects.contains("vanillabp.extensions.business-cockpit.rest.base-url"), defects);
    assertTrue(
        defects.contains("vanillabp.extensions.business-cockpit.kafka.bootstrap-servers"),
        defects);
    assertTrue(
        defects.contains("vanillabp.extensions.business-cockpit.kafka.topics.user-task"),
        defects);

  }

  @Test
  @DisplayName("With both transports the application is told to remove one")
  public void bothTransportsAreRefused() {

    final var defects = defectsOf(
        ConfigurationFixture
            .aConfiguredApplication()
            .with("kafka.bootstrap-servers", "broker:9092")
            .with("kafka.topics.user-task", "user-task")
            .with("kafka.topics.workflow", "workflow")
            .with("kafka.topics.workflow-module", "workflow-module"),
        false);

    assertTrue(defects.contains("reported twice"), defects);
    assertTrue(defects.contains("rest.base-url"), defects);

  }

  @Test
  @DisplayName("Kafka without all three topics names the ones which are missing")
  public void kafkaWithoutTopicsNamesThem() {

    final var defects = defectsOf(
        ConfigurationFixture
            .aConfiguredApplication()
            .without("rest.base-url")
            .with("kafka.bootstrap-servers", "broker:9092")
            .with("kafka.topics.user-task", "user-task"),
        false);

    assertTrue(defects.contains("kafka.topics.workflow"), defects);
    assertTrue(defects.contains("kafka.topics.workflow-module"), defects);

  }

  @Test
  @DisplayName("Without templates the BPMN language is mandatory, with templates it is not")
  public void bpmnDescriptionLanguageIsMandatoryWithoutTemplates() {

    final var withoutTemplates = ConfigurationFixture
        .aConfiguredApplication()
        .withoutWorkflowModule("bpmn-description-language");

    final var defects = defectsOf(withoutTemplates, true);
    assertTrue(defects.contains("bpmn-description-language"), defects);
    assertTrue(defects.contains("template-loader-path"), defects);

    final var withTemplates = ConfigurationFixture
        .aConfiguredApplication()
        .withoutWorkflowModule("bpmn-description-language")
        .with("template-loader-path", "/tmp/templates");
    assertNull(
        read(withTemplates, true)
            .workflowModule(ConfigurationFixture.WORKFLOW_MODULE)
            .bpmnDescriptionLanguage());

  }

  @Test
  @DisplayName("A template path without a template engine is reported rather than ignored")
  public void templatePathWithoutAnEngineIsReported() {

    final var defects = defectsOf(
        ConfigurationFixture.aConfiguredApplication().with("template-loader-path", "/tmp"),
        false);

    assertTrue(defects.contains("org.freemarker:freemarker"), defects);

  }

  @Test
  @DisplayName("Every missing setting of a workflow module is named in one message")
  public void missingWorkflowModuleSettingsAreNamedAtOnce() {

    final var defects = defectsOf(
        ConfigurationFixture
            .anApplication()
            .with("rest.base-url", "http://localhost:8080")
            .withWorkflowModule("workflow-module-uri", "http://localhost:8081"),
        false);

    assertTrue(defects.contains("ui-uri-type"), defects);
    assertTrue(defects.contains("ui-uri-path"), defects);
    assertTrue(defects.contains("i18n-languages"), defects);
    assertTrue(defects.contains("bpmn-description-language"), defects);
    assertTrue(
        defects.contains("vanillabp.workflow-modules.test-module.extensions.business-cockpit"),
        defects);

  }

  @Test
  @DisplayName("A workflow module which says nothing about the cockpit reports nothing")
  public void aSilentWorkflowModuleTakesNoPart() {

    final var configuration = read(
        ConfigurationFixture.anApplication().with("rest.base-url", "http://localhost:8080"),
        false);

    assertFalse(configuration.reportsToTheCockpit(ConfigurationFixture.WORKFLOW_MODULE));
    final var failure = assertThrows(
        IllegalStateException.class,
        () -> configuration.workflowModule(ConfigurationFixture.WORKFLOW_MODULE));
    assertTrue(
        failure
            .getMessage()
            .contains(
                "vanillabp.workflow-modules.test-module.extensions.business-cockpit.workflow-module-uri"),
        failure.getMessage());

  }

  @Test
  @DisplayName("A UI URI type naming nothing known lists the known ones")
  public void unknownUiUriTypeListsTheKnownOnes() {

    final var defects = defectsOf(
        ConfigurationFixture.aConfiguredApplication().withWorkflowModule("ui-uri-type", "IFRAME"),
        false);

    assertTrue(defects.contains("EXTERNAL"), defects);
    assertTrue(defects.contains("WEBPACK_MF_REACT"), defects);

  }

  @Test
  @DisplayName("A workflow module's value wins over the extension's global one, per key")
  public void theWorkflowModulesValueWins() {

    final var configuration = read(
        ConfigurationFixture
            .aConfiguredApplication()
            .with("ui-uri-path", "/global.js")
            .with("template-path", "shared")
            .withWorkflowModule("ui-uri-path", "/module.js"),
        false);

    final var module = configuration.workflowModule(ConfigurationFixture.WORKFLOW_MODULE);
    assertEquals("/module.js", module.uiUriPath());
    assertEquals("shared", module.templatePath());

  }

  @Test
  @DisplayName("The group hierarchy is read as one entry per group")
  public void groupHierarchyIsRead() {

    final var configuration = read(
        ConfigurationFixture
            .aConfiguredApplication()
            .withWorkflowModule("group-hierarchy.TEAM_LEAD", "TEAM_MEMBER, ASSISTANT"),
        false);

    assertEquals(
        List.of("TEAM_MEMBER", "ASSISTANT"),
        List
            .copyOf(
                configuration
                    .workflowModule(ConfigurationFixture.WORKFLOW_MODULE)
                    .groupHierarchy()
                    .get("TEAM_LEAD")));

  }

  @Test
  @DisplayName("A single workflow says what it differs from its module in")
  public void aWorkflowOverridesItsModule() {

    final var configuration = read(
        ConfigurationFixture
            .aConfiguredApplication()
            .withWorkflow("TaxiRide", "i18n-languages", "fr")
            .withWorkflow("TaxiRide", "bpmn-description-language", "fr")
            .withWorkflow("TaxiRide", "template-path", "taxi")
            .withUserTask("TaxiRide", "approve", "template-path", "approval"),
        false);

    final var module = configuration.workflowModule(ConfigurationFixture.WORKFLOW_MODULE);
    assertEquals(List.of("fr"), module.i18nLanguages("TaxiRide"));
    assertEquals("fr", module.bpmnDescriptionLanguage("TaxiRide"));
    assertEquals("taxi", module.templatePathOfWorkflow("TaxiRide"));
    assertEquals("approval", module.templatePathOfUserTask("TaxiRide", "approve"));

    // every other workflow, and every other task of this one, keeps what the module says
    assertEquals(List.of("en", "de"), module.i18nLanguages("Delivery"));
    assertEquals("en", module.bpmnDescriptionLanguage("Delivery"));
    assertEquals("Delivery", module.templatePathOfWorkflow("Delivery"));
    assertEquals("decide", module.templatePathOfUserTask("TaxiRide", "decide"));

  }

  @Test
  @DisplayName("A key which is no setting of a single workflow says where it belongs")
  public void aKeyWhichIsNoWorkflowSettingIsReported() {

    final var defects = defectsOf(
        ConfigurationFixture
            .aConfiguredApplication()
            .withWorkflow("TaxiRide", "ui-uri-path", "/taxi.js"),
        false);

    assertTrue(
        defects
            .contains(
                "vanillabp.workflow-modules.test-module.extensions.business-cockpit.workflows.TaxiRide.ui-uri-path"),
        defects);
    assertTrue(defects.contains("bpmn-description-language"), defects);

  }

  @Test
  @DisplayName("A key which is no setting of a single user task names the one which is")
  public void aKeyWhichIsNoUserTaskSettingIsReported() {

    final var defects = defectsOf(
        ConfigurationFixture
            .aConfiguredApplication()
            .withUserTask("TaxiRide", "approve", "i18n-languages", "fr"),
        false);

    assertTrue(
        defects
            .contains(
                "vanillabp.workflow-modules.test-module.extensions.business-cockpit.workflows.TaxiRide.user-tasks.approve.i18n-languages"),
        defects);
    assertTrue(defects.contains("template-path"), defects);

  }

  @Test
  @DisplayName("The REST client's timeouts, proxy, truststore and OAuth flow are read")
  public void theRestClientIsConfigured() {

    final var configuration = read(
        ConfigurationFixture
            .aConfiguredApplication()
            .with("rest.connect-timeout", "1500ms")
            .with("rest.read-timeout", "PT20S")
            .with("rest.proxy.host", "proxy.internal")
            .with("rest.proxy.port", "3128")
            .with("rest.proxy.username", "walter")
            .with("rest.proxy.password", "secret")
            .with("rest.verify-ssl", "false")
            .without("rest.username")
            .with("rest.authentication.oauth.base-url", "http://localhost:9000/token")
            .with("rest.authentication.oauth.client-id", "taxi-ride")
            .with("rest.authentication.oauth.client-secret", "s3cret")
            .with("rest.authentication.oauth.basic", "true"),
        false);

    final var rest = configuration.getRest();
    assertEquals(Duration.ofMillis(1500), rest.connectTimeout());
    assertEquals(Duration.ofSeconds(20), rest.readTimeout());
    assertEquals("proxy.internal", rest.proxy().host());
    assertEquals(3128, rest.proxy().port());
    assertTrue(rest.proxy().authenticates());
    assertFalse(rest.verifySsl());
    assertEquals("taxi-ride", rest.oauth().clientId());
    assertTrue(rest.oauth().clientInAuthorizationHeader());
    assertTrue(rest.needsAClientOfItsOwn());

  }

  @Test
  @DisplayName("A timeout which is no span of time is refused with both spellings")
  public void aBrokenTimeoutNamesBothSpellings() {

    final var defects = defectsOf(
        ConfigurationFixture.aConfiguredApplication().with("rest.read-timeout", "soon"), false);

    assertTrue(defects.contains("rest.read-timeout"), defects);
    assertTrue(defects.contains("PT10S"), defects);
    assertTrue(defects.contains("1500ms"), defects);

  }

  @Test
  @DisplayName("A proxy without a port, and a port without a proxy, are both reported")
  public void anIncompleteProxyIsReported() {

    assertTrue(
        defectsOf(
            ConfigurationFixture
                .aConfiguredApplication()
                .with("rest.proxy.host", "proxy.internal"),
            false).contains("rest.proxy.port"),
        "a proxy without a port");
    assertTrue(
        defectsOf(
            ConfigurationFixture.aConfiguredApplication().with("rest.proxy.port", "3128"),
            false).contains("rest.proxy.host"),
        "a port without a proxy");

  }

  @Test
  @DisplayName("A truststore which cannot be read ends the boot rather than the first report")
  public void anUnreadableTruststoreIsReported() {

    final var defects = defectsOf(
        ConfigurationFixture
            .aConfiguredApplication()
            .with("rest.ssl-truststore-filename", "/nowhere/cockpit.p12")
            .with("rest.ssl-truststore-password", "changeit"),
        false);

    assertTrue(defects.contains("/nowhere/cockpit.p12"), defects);
    assertTrue(defects.contains("running container"), defects);

  }

  @Test
  @DisplayName("A client-credentials flow missing a half, and one next to a user name, are refused")
  public void anIncompleteOauthFlowIsReported() {

    final var halfConfigured = defectsOf(
        ConfigurationFixture
            .aConfiguredApplication()
            .with("rest.authentication.oauth.base-url", "http://localhost:9000/token")
            .with("rest.authentication.oauth.client-id", "taxi-ride"),
        false);
    assertTrue(halfConfigured.contains("rest.authentication.oauth.client-secret"), halfConfigured);

    final var withoutTheServer = defectsOf(
        ConfigurationFixture
            .aConfiguredApplication()
            .with("rest.authentication.oauth.client-id", "taxi-ride"),
        false);
    assertTrue(withoutTheServer.contains("rest.authentication.oauth.base-url"), withoutTheServer);

    final var twoWays = defectsOf(
        ConfigurationFixture
            .aConfiguredApplication()
            .with("rest.username", "cockpit")
            .with("rest.authentication.oauth.base-url", "http://localhost:9000/token")
            .with("rest.authentication.oauth.client-id", "taxi-ride")
            .with("rest.authentication.oauth.client-secret", "s3cret"),
        false);
    assertTrue(twoWays.contains("bearer token"), twoWays);

  }

  @Test
  @DisplayName("Asking for a workflow module nobody configured names the ones which are")
  public void unknownWorkflowModuleNamesTheKnownOnes() {

    final var configuration = read(ConfigurationFixture.aConfiguredApplication(), false);

    final var message = assertThrows(
        IllegalStateException.class,
        () -> configuration.workflowModule("nowhere")).getMessage();
    assertTrue(message.contains(ConfigurationFixture.WORKFLOW_MODULE), message);

  }

}
