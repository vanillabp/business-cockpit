package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.config.InitiatorSource;
import io.vanillabp.cockpit.extension.config.UiUriType;
import io.vanillabp.cockpit.extension.config.WorkflowConfiguration;
import io.vanillabp.cockpit.extension.config.WorkflowModuleConfiguration;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Which level a setting comes from, for settings which a single workflow may set, too.
 * <p>
 * The platform defines the order of the levels and tests it. This test checks that the extension
 * uses that order with its own records as the sections, and that the value of a workflow beats
 * the value of its workflow module.
 */
@ExtendWith(SuppressOutputExtension.class)
public class WorkflowModuleSettingsTest {

  private static final String MODULE_ID = "test-module";

  private static final String PROCESS_ID = "TestProcess";

  private static WorkflowModuleConfiguration aModuleWhoseWorkflowSays(
      final WorkflowConfiguration workflow) {

    return new WorkflowModuleConfiguration(
        MODULE_ID, "http://localhost", UiUriType.EXTERNAL, "/ui", List.of("en"), "en", Map
            .of(), MODULE_ID, workflow == null ? Map.of()
                : Map.of(PROCESS_ID, workflow), InitiatorSource.BY_APPLICATION);

  }

  @Test
  @DisplayName("A workflow which says nothing is answered by its workflow module")
  public void theModuleAnswersForASilentWorkflow() {

    final var module = aModuleWhoseWorkflowSays(
        new WorkflowConfiguration(PROCESS_ID, null, null, null, Map.of(), null));

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
        new WorkflowConfiguration(PROCESS_ID, List.of("fr"), "fr", null, Map.of(), null));

    assertEquals(List.of("fr"), module.i18nLanguages(PROCESS_ID));
    assertEquals("fr", module.bpmnDescriptionLanguage(PROCESS_ID));
    // and the value holds for that workflow alone
    assertEquals(List.of("en"), module.i18nLanguages("SomeOtherProcess"));

  }

  @Test
  @DisplayName("A module which says nothing either answers nothing")
  public void nothingAnywhereIsNothing() {

    final var module = new WorkflowModuleConfiguration(
        MODULE_ID, "http://localhost", UiUriType.EXTERNAL, "/ui", null, null, Map.of(), MODULE_ID, Map.of(PROCESS_ID,
            new WorkflowConfiguration(PROCESS_ID, null, null, null, Map.of(), null)), null);

    assertEquals(List.of(), module.i18nLanguages(PROCESS_ID));
    assertNull(module.bpmnDescriptionLanguage(PROCESS_ID));

  }

}
