package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.config.UiUriType;
import io.vanillabp.cockpit.extension.config.WorkflowConfiguration;
import io.vanillabp.cockpit.extension.config.WorkflowModuleConfiguration;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Which level answers a setting a workflow may say something about.
 * <p>
 * The order of the levels is the platform's and is tested there. What is tested here is that this
 * extension asks through it. The sections it hands over are its own records, and a value of a
 * workflow has to beat the value of its workflow module.
 */
@ExtendWith(SuppressOutputExtension.class)
public class WorkflowModuleSettingsTest {

  private static final String MODULE_ID = "test-module";

  private static final String PROCESS_ID = "TestProcess";

  private static WorkflowModuleConfiguration aModuleWhoseWorkflowSays(
      final WorkflowConfiguration workflow) {

    return new WorkflowModuleConfiguration(
        MODULE_ID, "http://localhost", UiUriType.EXTERNAL, "/ui", List.of("en"), "en", Map
            .of(), MODULE_ID, workflow == null ? Map.of() : Map.of(PROCESS_ID, workflow));

  }

  @Test
  @DisplayName("A workflow which says nothing is answered by its workflow module")
  public void theModuleAnswersForASilentWorkflow() {

    final var module = aModuleWhoseWorkflowSays(
        new WorkflowConfiguration(PROCESS_ID, null, null, null, Map.of()));

    assertEquals(List.of("en"), module.i18nLanguages(PROCESS_ID));
    assertEquals("en", module.bpmnDescriptionLanguage(PROCESS_ID));

  }

  @Test
  @DisplayName("A workflow nobody configured is answered by its workflow module as well")
  public void theModuleAnswersForAnUnknownWorkflow() {

    final var module = aModuleWhoseWorkflowSays(null);

    assertEquals(List.of("en"), module.i18nLanguages(PROCESS_ID));
    assertEquals("en", module.bpmnDescriptionLanguage("SomeOtherProcess"));

  }

  @Test
  @DisplayName("What a workflow says beats what its workflow module says")
  public void theWorkflowBeatsItsModule() {

    final var module = aModuleWhoseWorkflowSays(
        new WorkflowConfiguration(PROCESS_ID, List.of("fr"), "fr", null, Map.of()));

    assertEquals(List.of("fr"), module.i18nLanguages(PROCESS_ID));
    assertEquals("fr", module.bpmnDescriptionLanguage(PROCESS_ID));
    // and it says it for itself alone
    assertEquals(List.of("en"), module.i18nLanguages("SomeOtherProcess"));

  }

  @Test
  @DisplayName("A module which says nothing either answers nothing")
  public void nothingAnywhereIsNothing() {

    final var module = new WorkflowModuleConfiguration(
        MODULE_ID, "http://localhost", UiUriType.EXTERNAL, "/ui", null, null, Map.of(), MODULE_ID, Map.of(PROCESS_ID,
            new WorkflowConfiguration(PROCESS_ID, null, null, null, Map.of())));

    assertEquals(List.of(), module.i18nLanguages(PROCESS_ID));
    assertNull(module.bpmnDescriptionLanguage(PROCESS_ID));

  }

}
