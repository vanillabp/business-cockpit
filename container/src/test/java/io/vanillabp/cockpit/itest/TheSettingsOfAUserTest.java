package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.jayway.jsonpath.JsonPath;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The user interface keeps the settings of a user at the cockpit, so they follow the user to
 * every browser. Each setting is read, written and removed on its own, and only by the user it
 * belongs to.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class TheSettingsOfAUserTest extends ItestBase {

  private static final String SETTINGS = "/app/current-user/settings/";

  /** MongoDB takes a document of at most 16 MB. */
  private static final int SIXTEEN_MEGABYTES = 16 * 1024 * 1024;

  private String martin;
  private String petra;

  @BeforeEach
  void login() {
    martin = loginToGui(USER_MARTIN);
    petra = loginToGui(USER_PETRA);
  }

  @Test
  void aSettingIsReadAsItWasWritten() {

    final var values = List.of(
        "42", "\"de\"", "true", "{ \"amount\": 120, \"title\": [ 1, 2 ] }", "[ \"title\", \"dueDate\" ]");
    final var keys = new ArrayList<String>();
    for (final var value : values) {
      final var key = unique("setting");
      keys.add(key);
      assertThat(guiPut(martin, SETTINGS + key, value).statusCode()).isEqualTo(204);
    }

    for (var i = 0; i < values.size(); ++i) {
      final var read = guiGet(martin, SETTINGS + keys.get(i));
      assertThat(read.statusCode()).isEqualTo(200);
      assertThat(read.headers().firstValue("Content-Type")).hasValueSatisfying(
          type -> assertThat(type).startsWith("application/json"));
      assertThat((Object) JsonPath.parse(read.body()).read("$"))
          .as("the value of %s", values.get(i))
          .isEqualTo(JsonPath.parse(values.get(i)).read("$"));
    }
    // a text has to come back as a JSON string, in quotes, and not as the bare text
    assertThat(guiGet(martin, SETTINGS + keys.get(1)).body()).isEqualTo("\"de\"");

  }

  @Test
  void aWriteReplacesTheValueAndADeleteRemovesIt() {

    final var key = "list:columns-"
        + unique("x");
    guiPut(martin, SETTINGS + key, "[ \"title\" ]");
    assertThat(guiPut(martin, SETTINGS + key, "[ \"dueDate\" ]").statusCode()).isEqualTo(204);
    assertThat((Object) JsonPath.parse(guiGet(martin, SETTINGS + key).body()).read("$")).isEqualTo(List.of("dueDate"));

    assertThat(guiDelete(martin, SETTINGS + key).statusCode()).isEqualTo(204);
    assertThat(guiGet(martin, SETTINGS + key).statusCode()).isEqualTo(404);
    assertThat(guiDelete(martin, SETTINGS + key).statusCode())
        .as("removing what is not there is no mistake")
        .isEqualTo(204);

  }

  @Test
  void nullIsNoValue() {

    final var key = unique("setting");
    guiPut(martin, SETTINGS + key, "1");

    final var withNull = guiPut(martin, SETTINGS + key, "null");
    assertThat(withNull.statusCode()).isEqualTo(400);
    assertThat(withNull.body()).contains("DELETE");
    assertThat(send(HttpRequest
        .newBuilder(URI.create(url("/gui/api/v1"
            + SETTINGS
            + key)))
        .header("Cookie", martin)
        .PUT(HttpRequest.BodyPublishers.noBody())
        .build()).statusCode()).as("no body at all").isEqualTo(400);

    assertThat(guiGet(martin, SETTINGS + key).body()).as("the value stays").isEqualTo("1");

  }

  /**
   * The chain itself refuses the request, before any controller sees it. Its answer asks for
   * basic authentication. A request which got through to the controller would get a 401 too, but
   * without that header, so the header tells that the path is not open to everybody.
   */
  @Test
  void nobodySignedInGetsNothing() {

    final var key = unique("setting");
    guiPut(martin, SETTINGS + key, "1");

    for (final var request : List.of(
        HttpRequest.newBuilder(URI.create(url("/gui/api/v1"
            + SETTINGS
            + key))).GET().build(),
        HttpRequest.newBuilder(URI.create(url("/gui/api/v1"
            + SETTINGS
            + key)))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString("2"))
            .build(),
        HttpRequest.newBuilder(URI.create(url("/gui/api/v1"
            + SETTINGS
            + key))).DELETE().build())) {
      final var response = send(request);
      assertThat(response.statusCode()).as(request.method()).isEqualTo(401);
      assertThat(response.headers().firstValue("WWW-Authenticate"))
          .as("the chain refused the %s", request.method())
          .hasValueSatisfying(value -> assertThat(value).startsWith("Basic"));
    }

    assertThat(guiGet(martin, SETTINGS + key).body()).isEqualTo("1");

  }

  @Test
  void eachUserSeesTheirOwnSettingsOnly() {

    final var key = unique("setting");
    guiPut(martin, SETTINGS + key, "\"martin's\"");

    assertThat(guiGet(petra, SETTINGS + key).statusCode()).isEqualTo(404);
    guiPut(petra, SETTINGS + key, "\"petra's\"");
    guiDelete(petra, SETTINGS + key);

    assertThat(guiGet(martin, SETTINGS + key).body()).isEqualTo("\"martin's\"");

  }

  @Test
  void writesOfDifferentKeysAtTheSameTimeLoseNothing() throws Exception {

    final var prefix = unique("tab");
    final var writes = 24;
    try (var pool = Executors.newFixedThreadPool(8)) {
      final var futures = IntStream
          .range(0, writes)
          .mapToObj(number -> CompletableFuture.supplyAsync(() -> guiPut(martin, SETTINGS
              + prefix
              + "-"
              + number, Integer.toString(number)).statusCode(), pool))
          .toList();
      for (final var future : futures) {
        assertThat(future.get()).isEqualTo(204);
      }
    }

    for (var number = 0; number < writes; ++number) {
      assertThat(guiGet(martin, SETTINGS
          + prefix
          + "-"
          + number).body()).isEqualTo(Integer.toString(number));
    }

  }

  @Test
  void aSettingAndTheNotificationConfigurationKeepEachOther() {

    final var configuration = """
        { "globalAllViaMedium": { "email": true } }""";
    assertThat(guiPut(petra, "/notifications/config", configuration).statusCode()).isEqualTo(200);

    final var key = unique("setting");
    guiPut(petra, SETTINGS + key, "\"de\"");
    assertThat(json(guiGet(petra, "/notifications/config")).read("$.globalAllViaMedium.email", Boolean.class))
        .as("the setting leaves the notification configuration as it was")
        .isTrue();

    // the notification configuration is saved as a whole document, which has to carry the
    // settings along
    assertThat(guiPut(petra, "/notifications/config", """
        { "globalAllViaMedium": { "email": false } }""").statusCode()).isEqualTo(200);
    assertThat(guiGet(petra, SETTINGS + key).body()).isEqualTo("\"de\"");

  }

  @Test
  void aKeyMongoDbCannotTakeIsRefused() {

    for (final var key : List.of(
        "a.b", "%24where", "with%20space", "x".repeat(201))) {
      final var response = guiPut(martin, SETTINGS + key, "1");
      assertThat(response.statusCode()).as(key).isEqualTo(400);
      assertThat(response.body()).as(key).contains("'key'");
      assertThat(guiGet(martin, SETTINGS + key).statusCode()).as(key).isEqualTo(400);
      assertThat(guiDelete(martin, SETTINGS + key).statusCode()).as(key).isEqualTo(400);
    }
    assertThat(guiPut(martin, SETTINGS + "x".repeat(200), "1").statusCode()).as("200 letters").isEqualTo(204);

  }

  /**
   * The notification configuration is saved as a whole document, through the mapping of Spring
   * Data. That mapping refuses a key with a dot in a map. Such a key inside a setting would break
   * every later save of the notification configuration, so the setting is refused.
   */
  @Test
  void aKeyInsideTheValueWhichMongoDbCannotTakeIsRefused() {

    final var key = unique("setting");
    for (final var value : List.of(
        "{ \"details.amount\": 120 }", "{ \"widths\": [ { \"$size\": 3 } ] }")) {
      final var written = guiPut(martin, SETTINGS + key, value);
      assertThat(written.statusCode()).as(value).isEqualTo(400);
      assertThat(written.body()).as(value).contains("may not hold a dot or start with '$'");
    }

    assertThat(guiGet(martin, SETTINGS + key).statusCode()).isEqualTo(404);
    assertThat(guiPut(martin, "/notifications/config", """
        { "globalAllViaMedium": { "email": true } }""").statusCode())
        .as("the notification configuration can still be saved")
        .isEqualTo(200);

  }

  /**
   * There is no limit of the cockpit. A setting is refused where MongoDB refuses the document:
   * one value which is too large alone, and one which only makes the document too large.
   */
  @Test
  void settingsLargerThanMongoDbTakesAreRefused() {

    final var tooLargeAlone = guiPut(petra, SETTINGS + unique("large"), "\""
        + "x".repeat(SIXTEEN_MEGABYTES + 1)
        + "\"");
    assertThat(tooLargeAlone.statusCode()).isEqualTo(413);
    assertThat(tooLargeAlone.body()).contains("16 MB");

    final var half = "\""
        + "x".repeat(SIXTEEN_MEGABYTES / 2 + 1)
        + "\"";
    final var first = unique("half");
    assertThat(guiPut(petra, SETTINGS + first, half).statusCode()).isEqualTo(204);
    final var second = guiPut(petra, SETTINGS + unique("half"), half);
    assertThat(second.statusCode()).isEqualTo(413);
    assertThat(second.body()).contains("16 MB");

    // the other tests of this class share the user, so the space is given back
    guiDelete(petra, SETTINGS + first);
    final var key = unique("small");
    assertThat(guiPut(petra, SETTINGS + key, "1").statusCode()).isEqualTo(204);

  }

  private HttpResponse<String> guiDelete(
      final String cookie,
      final String path) {

    return send(HttpRequest
        .newBuilder(URI.create(url("/gui/api/v1"
            + path)))
        .header("Cookie", cookie)
        .DELETE()
        .build());

  }

}
