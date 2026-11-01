package io.vanillabp.cockpit.gui.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.yaml.snakeyaml.Yaml;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The update stream is written by hand, so nothing ties the cockpit's OpenAPI file to it. These
 * tests read the file as this jar carries it and keep it next to the code. They also check the two
 * things the generator is configured for: it writes no interface for the stream, and it writes no
 * second {@code Person} and {@code Group}.
 */
@ExtendWith(SuppressOutputExtension.class)
class UpdateStreamIsInTheOpenApiFileTest {

  @Test
  @SuppressWarnings("unchecked")
  void theFileDescribesTheStreamAsAnEventStream() throws IOException {

    final Map<String, Object> spec;
    try (var in = getClass().getClassLoader().getResourceAsStream("api/gui/v1.yaml")) {
      assertThat(in).as("the jar carries its OpenAPI file").isNotNull();
      spec = new Yaml().load(in);
    }

    final var operation = (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) spec
        .get("paths"))
        .get("/updates"))
        .get("get");
    assertThat(operation).as("GET /updates is in the file").isNotNull();
    assertThat(operation.get("x-internal"))
        .as("the generator must not write an interface for the stream")
        .isEqualTo(true);
    assertThat((Map<String, Object>) operation.get("x-event-types"))
        .containsOnlyKeys("UserTask", "Workflow", "ping");

    final var content = (Map<String, Object>) ((Map<String, Object>) ((Map<Object, Object>) operation
        .get("responses"))
        .get(200))
        .get("content");
    assertThat(content).containsOnlyKeys("text/event-stream");

  }

  /**
   * A generated {@code UpdatesApi} would map {@code GET /gui/api/v1/updates} a second time, next
   * to {@link LoginApiController}, and the cockpit would not start.
   */
  @Test
  void nothingIsGeneratedForTheStream() {

    assertThatThrownBy(() -> Class.forName("io.vanillabp.cockpit.gui.api.v1.UpdatesApi"))
        .isInstanceOf(ClassNotFoundException.class);

  }

  @Test
  void personAndGroupAreOnTheClassPathOnce() throws IOException {

    final var loader = getClass().getClassLoader();
    assertThat(Collections.list(loader.getResources("io/vanillabp/cockpit/gui/api/v1/Group.class")))
        .hasSize(1);
    assertThat(Collections.list(loader.getResources("io/vanillabp/cockpit/gui/api/v1/Person.class")))
        .hasSize(1);

  }

}
