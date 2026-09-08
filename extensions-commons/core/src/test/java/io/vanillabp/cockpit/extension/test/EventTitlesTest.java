package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import freemarker.cache.ClassTemplateLoader;
import io.vanillabp.cockpit.extension.config.UiUriType;
import io.vanillabp.cockpit.extension.config.WorkflowModuleConfiguration;
import io.vanillabp.cockpit.extension.event.UserTaskEvent;
import io.vanillabp.cockpit.extension.event.WorkflowEvent;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.cockpit.extension.templating.EventTitles;
import io.vanillabp.cockpit.extension.templating.FreemarkerTemplating;
import io.vanillabp.cockpit.extension.templating.Templating;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What the cockpit shows as a title, and where it comes from.
 * <p>
 * The template context of these tests is a Java record, which is the shape a workflow module
 * naturally builds for a title and the one Freemarker only reads when the object wrapper is
 * configured for it.
 */
@ExtendWith(SuppressOutputExtension.class)
public class EventTitlesTest {

  /** The little view object a workflow module hands to its templates. */
  public record Order(
                      String number,
                      Integer amount) {
  }

  private static final WorkflowModuleConfiguration MODULE = new WorkflowModuleConfiguration(
      "test-module", "http://localhost", UiUriType.EXTERNAL, "/ui", List.of("en"), "en", Map.of(), "test-module");

  private static Templating templating() {

    final var configuration = FreemarkerTemplating.newConfiguration();
    configuration.setTemplateLoader(new ClassTemplateLoader(EventTitlesTest.class, "/templates"));
    return new FreemarkerTemplating(configuration);

  }

  private static UserTaskEvent userTaskEvent() {

    final var event = new UserTaskEvent(UserTaskEventKind.CREATED);
    event.setWorkflowModuleId("test-module");
    event.setBpmnProcessId("TestProcess");
    event.setTaskDefinition("approve");
    return event;

  }

  @Test
  @DisplayName("Titles are rendered from the most specific template, reading a record")
  public void titlesAreRenderedFromTemplates() {

    final var event = userTaskEvent();
    event.setTemplateContext(Map.of("order", new Order("4711", 250)));

    EventTitles.fill(event, MODULE, templating(), "Approve", "Order handling");

    assertEquals("Order 4711 over 250", event.getWorkflowTitle().get("en"));
    assertEquals("Approve order 4711", event.getTitle().get("en"));
    assertEquals("Approval", event.getTaskDefinitionTitle().get("en"));
    assertEquals("Order 4711 over 250 / Approve order 4711", event.getDetailsFulltextSearch());

  }

  @Test
  @DisplayName("A value the template did not fill stays absent rather than becoming an error")
  public void anAbsentRecordComponentIsSkipped() {

    final var event = userTaskEvent();
    event.setTemplateContext(Map.of("order", new Order("4712", null)));

    EventTitles.fill(event, MODULE, templating(), "Approve", "Order handling");

    assertEquals("Order 4712", event.getWorkflowTitle().get("en"));

  }

  @Test
  @DisplayName("Without templates the BPMN names are reported, in the BPMN's own language")
  public void withoutTemplatesTheBpmnNamesAreReported() {

    final var event = userTaskEvent();

    EventTitles.fill(event, MODULE, Templating.none(), "Approve", "Order handling");

    assertEquals("Order handling", event.getWorkflowTitle().get("en"));
    assertEquals("Approve", event.getTitle().get("en"));
    assertEquals("Approve", event.getTaskDefinitionTitle().get("en"));
    assertNull(event.getDetailsFulltextSearch());
    assertEquals(List.of("en"), event.getI18nLanguages());

  }

  @Test
  @DisplayName("What a details provider wrote is treated as a template name and survives as text")
  public void whatTheDetailsProviderWroteSurvives() {

    final var event = userTaskEvent();
    event.setTitle(Map.of("en", "Please approve"));

    EventTitles.fill(event, MODULE, templating(), "Approve", "Order handling");

    assertEquals("Please approve", event.getTitle().get("en"));

  }

  @Test
  @DisplayName("An immutable map handed in by a details provider is replaced instead of failing")
  public void anImmutableTitleMapIsReplaced() {

    final var event = userTaskEvent();
    event.setWorkflowTitle(Map.of());
    event.setTemplateContext(Map.of("order", new Order("4713", 10)));

    EventTitles.fill(event, MODULE, templating(), "Approve", "Order handling");

    assertEquals("Order 4713 over 10", event.getWorkflowTitle().get("en"));

  }

  @Test
  @DisplayName("A workflow's title is rendered from the module's template")
  public void workflowTitleIsRendered() {

    final var event = new WorkflowEvent(WorkflowEventKind.CREATED);
    event.setWorkflowModuleId("test-module");
    event.setBpmnProcessId("TestProcess");
    event.setTemplateContext(Map.of("order", new Order("4714", 99)));

    EventTitles.fill(event, MODULE, templating(), "Order handling");

    assertEquals("Order 4714 over 99", event.getTitle().get("en"));
    assertEquals("Order 4714 over 99", event.getDetailsFulltextSearch());

  }

  @Test
  @DisplayName("The lookup path narrows from the module to the process to the task definition")
  public void lookupPathsNarrow() {

    final var paths = EventTitles.lookupPaths(MODULE, "TestProcess", "approve");

    assertEquals(
        List.of("test-module/TestProcess/approve", "test-module/TestProcess", "test-module"),
        paths);
    assertTrue(EventTitles.lookupPaths(MODULE, null, null).equals(List.of("test-module")));

  }

}
