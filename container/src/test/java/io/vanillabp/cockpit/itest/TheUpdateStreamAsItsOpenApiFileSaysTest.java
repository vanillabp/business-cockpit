package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.jayway.jsonpath.JsonPath;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Reads the update stream of a running cockpit and holds it to what the cockpit's OpenAPI file
 * says about it: the names of the events, an id per event, and the fields of the data. The file
 * describes the stream in words only, because OpenAPI 3.0 cannot describe the events. So nothing
 * else notices when the stream and the file drift apart.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class TheUpdateStreamAsItsOpenApiFileSaysTest extends ItestBase {

  @Test
  void aPingCarriesAnEmptyObject() {

    final var lines = openEventStream(loginToGui(USER_MARTIN));

    await().untilAsserted(() -> assertThat(eventsNamed("ping", lines)).isNotEmpty());

    final var ping = eventsNamed("ping", lines).get(0);
    assertThat(ping.id()).as("every event has an id").isNotBlank();
    assertThat(JsonPath.<Map<String, Object>>read(ping.data(), "$")).isEmpty();

  }

  @Test
  void aNewStreamAsksEveryListToReload() {

    final var lines = openEventStream(loginToGui(USER_MARTIN));

    await().untilAsserted(() -> {
      assertThat(typesIn(eventsNamed("UserTask", lines))).contains("RELOAD");
      assertThat(typesIn(eventsNamed("Workflow", lines))).contains("RELOAD");
    });

    final var reload = eventsNamed("UserTask", lines).get(0);
    assertThat(JsonPath.<String>read(reload.data(), "$[0].event.id"))
        .as("a reload is about no single task")
        .isEmpty();

  }

  @Test
  void aChangedUserTaskIsAnArrayOfWakeUpCalls() {

    final var moduleId = unique("stream-shape-module");
    registerWorkflowModule(moduleId, "http://localhost:65000");
    final var lines = openEventStream(loginToGui(USER_MARTIN));
    await().untilAsserted(() -> assertThat(eventsNamed("ping", lines)).isNotEmpty());

    final var userTaskId = reportUserTask(moduleId, unique("token"), unique("workflow"), """
        "assignee": "%s"
        """.formatted(USER_MARTIN));

    await().untilAsserted(() -> assertThat(elementsAbout(userTaskId, "UserTask", lines))
        .isNotEmpty());
    final var element = elementsAbout(userTaskId, "UserTask", lines).get(0);
    assertThat(element).containsOnlyKeys("event", "source", "timestamp");
    assertThat(element.get("source")).isEqualTo("UserTask");
    assertThat(element.get("timestamp")).isInstanceOf(Number.class);
    assertThat(element.get("event")).isEqualTo(Map.of(
        "id", userTaskId, "name", "UserTask", "type", "INSERT"));

  }

  @Test
  void aChangedWorkflowIsAnArrayOfWakeUpCalls() {

    final var moduleId = unique("stream-shape-module");
    registerWorkflowModule(moduleId, "http://localhost:65000");
    final var lines = openEventStream(loginToGui(USER_MARTIN));
    await().untilAsserted(() -> assertThat(eventsNamed("ping", lines)).isNotEmpty());

    final var workflowId = unique("workflow");
    final var created = bpmsV1_1("/workflow/created", """
        {
          "id": "%s",
          "workflowId": "%s",
          "timestamp": "%s",
          "workflowModuleId": "%s",
          "bpmnProcessId": "taxi-ride",
          "bpmnProcessVersion": "1",
          "initiator": "martin",
          "title": { "en": "Ride request" },
          "uiUriPath": "/remoteEntry.js",
          "uiUriType": "WEBPACK_MF_REACT",
          "accessibleToGroups": [ "%s" ]
        }
        """.formatted(unique("event"), workflowId, isoNow(), moduleId, GROUP_OF_MARTIN));
    assertThat(created.statusCode()).isEqualTo(200);

    await().untilAsserted(() -> assertThat(elementsAbout(workflowId, "Workflow", lines))
        .isNotEmpty());
    final var element = elementsAbout(workflowId, "Workflow", lines).get(0);
    assertThat(element).containsOnlyKeys("event", "source", "timestamp");
    assertThat(element.get("source")).isEqualTo("Workflow");
    assertThat(element.get("timestamp")).isInstanceOf(Number.class);
    assertThat(element.get("event")).isEqualTo(Map.of(
        "id", workflowId, "name", "Workflow", "type", "INSERT"));

  }

  /** One event of the stream, its data put together from all its {@code data:} lines. */
  private record Event(String id, String name, String data) {
  }

  /**
   * Splits the lines into events. An empty line ends an event, as the format of server-sent
   * events says. An event still being written is left out.
   */
  private static List<Event> events(
      final Queue<String> lines) {

    final var result = new ArrayList<Event>();
    final var fields = new LinkedHashMap<String, StringBuilder>();
    for (final var line : List.copyOf(lines)) {
      if (line.isEmpty()) {
        if (!fields.isEmpty()) {
          result.add(new Event(
              valueOf(fields, "id"), valueOf(fields, "event"), valueOf(fields, "data")));
        }
        fields.clear();
        continue;
      }
      final var colon = line.indexOf(':');
      final var field = line.substring(0, colon);
      final var value = line.substring(colon + 1);
      fields.merge(field, new StringBuilder(value), (
          known,
          more) -> known.append('\n').append(more));
    }
    return result;

  }

  private static String valueOf(
      final Map<String, StringBuilder> fields,
      final String field) {

    final var value = fields.get(field);
    return value == null ? null : value.toString();

  }

  private static List<Event> eventsNamed(
      final String name,
      final Queue<String> lines) {

    return events(lines)
        .stream()
        .filter(event -> name.equals(event.name()))
        .toList();

  }

  private static List<String> typesIn(
      final List<Event> events) {

    return events
        .stream()
        .flatMap(event -> JsonPath.<List<String>>read(event.data(), "$[*].event.type").stream())
        .toList();

  }

  private static List<Map<String, Object>> elementsAbout(
      final String id,
      final String name,
      final Queue<String> lines) {

    return eventsNamed(name, lines)
        .stream()
        .flatMap(event -> JsonPath
            .<List<Map<String, Object>>>read(event.data(), "$[?(@.event.id == '"
                + id
                + "')]")
            .stream())
        .toList();

  }

}
