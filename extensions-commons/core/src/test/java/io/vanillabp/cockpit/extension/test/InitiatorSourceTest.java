package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.config.BusinessCockpitConfiguration;
import io.vanillabp.cockpit.extension.config.ConfigurationKeys;
import io.vanillabp.cockpit.extension.config.InitiatorSource;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What the configuration says about the initiator of a report.
 * <p>
 * The key has no default, so a workflow module which reports to the cockpit and answers nowhere
 * ends the boot. These assertions are about that message, because it is the whole point of the
 * key: a developer reads it once, at the first start, and answers it for good.
 */
@ExtendWith(SuppressOutputExtension.class)
public class InitiatorSourceTest {

  private static final String PROCESS_ID = "TestProcess";

  private static BusinessCockpitConfiguration read(
      final ConfigurationFixture fixture) {

    return BusinessCockpitConfiguration
        .readAndValidate(fixture.properties(), fixture.settings(), false, false);

  }

  private static String defectsOf(
      final ConfigurationFixture fixture) {

    return assertThrows(IllegalStateException.class, () -> read(fixture)).getMessage();

  }

  @Test
  @DisplayName("A reporting module which answers nowhere ends the boot, naming both keys")
  public void aModuleWithoutAnAnswerEndsTheBoot() {

    final var defects = defectsOf(
        ConfigurationFixture
            .aConfiguredApplication()
            .without(ConfigurationKeys.INITIATOR_SOURCE));

    assertTrue(
        defects
            .contains(
                ConfigurationKeys
                    .workflowModuleKey(
                        ConfigurationFixture.WORKFLOW_MODULE,
                        ConfigurationKeys.INITIATOR_SOURCE)),
        defects);
    assertTrue(
        defects.contains(ConfigurationKeys.globalKey(ConfigurationKeys.INITIATOR_SOURCE)),
        defects);
    assertTrue(defects.contains(ConfigurationFixture.WORKFLOW_MODULE), defects);
    // what the initiator is, and that it is gone for good once a case ran without it. Both
    // belong in the message: they are why somebody answers the question instead of silencing it
    assertTrue(defects.contains("the user who caused"), defects);
    assertTrue(defects.contains("for good"), defects);
    // and both answers, so that nothing has to be looked up
    assertTrue(defects.contains(InitiatorSource.BY_APPLICATION.configuredAs()), defects);
    assertTrue(defects.contains(InitiatorSource.SYSTEM.configuredAs()), defects);
    assertTrue(defects.contains("@UserTaskDetailsProvider"), defects);

  }

  @Test
  @DisplayName("A module which reports nothing is not asked")
  public void aModuleWhichReportsNothingIsNotAsked() {

    final var configuration = assertDoesNotThrow(
        () -> read(
            ConfigurationFixture
                .anApplication()
                .with(ConfigurationKeys.REST_BASE_URL, "http://localhost:8080")));

    assertNull(
        configuration.initiatorSource(ConfigurationFixture.WORKFLOW_MODULE, PROCESS_ID),
        "a module which takes no part in the cockpit was asked about its initiator");

  }

  @Test
  @DisplayName("The application answers for every module which says nothing itself")
  public void theApplicationAnswersForItsModules() {

    final var configuration = read(
        ConfigurationFixture
            .aConfiguredApplication()
            .with(ConfigurationKeys.INITIATOR_SOURCE, InitiatorSource.SYSTEM.configuredAs()));

    assertEquals(
        InitiatorSource.SYSTEM,
        configuration.initiatorSource(ConfigurationFixture.WORKFLOW_MODULE, PROCESS_ID));

  }

  @Test
  @DisplayName("A workflow beats its module, and a module beats the application")
  public void theMostSpecificLevelWins() {

    final var configuration = read(
        ConfigurationFixture
            .aConfiguredApplication()
            .with(ConfigurationKeys.INITIATOR_SOURCE, InitiatorSource.SYSTEM.configuredAs())
            .withWorkflowModule(
                ConfigurationKeys.INITIATOR_SOURCE,
                InitiatorSource.BY_APPLICATION.configuredAs())
            .withWorkflow(
                PROCESS_ID, ConfigurationKeys.INITIATOR_SOURCE,
                InitiatorSource.SYSTEM.configuredAs()));

    assertEquals(
        InitiatorSource.SYSTEM,
        configuration.initiatorSource(ConfigurationFixture.WORKFLOW_MODULE, PROCESS_ID),
        "the workflow's own value");
    assertEquals(
        InitiatorSource.BY_APPLICATION,
        configuration.initiatorSource(ConfigurationFixture.WORKFLOW_MODULE, "AnotherProcess"),
        "the module's value for every other workflow");

  }

  @Test
  @DisplayName("A value which is neither of the two names both, at every level")
  public void anUnknownValueNamesBothValues() {

    final var ofTheApplication = defectsOf(
        ConfigurationFixture
            .aConfiguredApplication()
            .with(ConfigurationKeys.INITIATOR_SOURCE, "whoever"));

    assertTrue(ofTheApplication.contains("whoever"), ofTheApplication);
    assertTrue(
        ofTheApplication.contains(InitiatorSource.BY_APPLICATION.configuredAs()),
        ofTheApplication);
    assertTrue(ofTheApplication.contains(InitiatorSource.SYSTEM.configuredAs()), ofTheApplication);

    final var ofTheModule = defectsOf(
        ConfigurationFixture
            .aConfiguredApplication()
            .withWorkflowModule(ConfigurationKeys.INITIATOR_SOURCE, "BY_APPLICATION"));

    assertTrue(
        ofTheModule
            .contains(
                ConfigurationKeys
                    .workflowModuleKey(
                        ConfigurationFixture.WORKFLOW_MODULE,
                        ConfigurationKeys.INITIATOR_SOURCE)),
        ofTheModule);
    assertTrue(ofTheModule.contains("BY_APPLICATION"), ofTheModule);

    final var ofTheWorkflow = defectsOf(
        ConfigurationFixture
            .aConfiguredApplication()
            .withWorkflow(PROCESS_ID, ConfigurationKeys.INITIATOR_SOURCE, "the-engine"));

    assertTrue(
        ofTheWorkflow
            .contains(
                ConfigurationKeys
                    .workflowKey(
                        ConfigurationFixture.WORKFLOW_MODULE, PROCESS_ID,
                        ConfigurationKeys.INITIATOR_SOURCE)),
        ofTheWorkflow);
    assertTrue(ofTheWorkflow.contains("the-engine"), ofTheWorkflow);

  }

}
