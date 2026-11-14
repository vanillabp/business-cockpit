package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.jayway.jsonpath.DocumentContext;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A user interface may show a list which holds the entries of some workflow modules only, for
 * example a page which belongs to one business application. It sends the ids of those modules as
 * {@code workflowModuleIds} with every request of the list: the page, the update, the count and
 * the suggestions of a search field. Without them, and with an empty array, the list holds the
 * entries of every workflow module.
 *
 * <p>Two modules report the same tasks and cases. Martin is the assignee of one task of each
 * module, and his group is the candidate of another. Every list is filtered by a token, so the
 * lists hold only what this test reported.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class AListLimitedToWorkflowModulesTest extends ItestBase {

  private String token;

  private String moduleA;

  private String moduleB;

  /** A word only the entries of module A carry in their full text. */
  private String wordOfModuleA;

  /** A word only the entries of module B carry in their full text. */
  private String wordOfModuleB;

  /** The part both words have in common, which the user types into a search field. */
  private String typed;

  private String martin;

  private final Map<String, String> assignedToMartin = new HashMap<>();

  private final Map<String, String> offeredToHisGroup = new HashMap<>();

  private final Map<String, String> workflows = new HashMap<>();

  @BeforeEach
  void reportTheSameEntriesInTwoModules() {

    token = unique("limited");
    typed = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    wordOfModuleA = "alpha"
        + typed;
    wordOfModuleB = "beta"
        + typed;
    moduleA = unique("module-a");
    moduleB = unique("module-b");
    martin = loginToGui(USER_MARTIN);

    for (final var module : List.of(moduleA, moduleB)) {
      registerWorkflowModule(module, "http://localhost:65000");
      final var fullText = token
          + " "
          + (module.equals(moduleA) ? wordOfModuleA : wordOfModuleB);
      final var workflowId = unique("workflow");
      reportWorkflow(module, fullText, workflowId, "created");
      workflows.put(module, workflowId);
      assignedToMartin.put(module, reportUserTask(module, fullText, workflowId, """
          "assignee": "%s"
          """.formatted(USER_MARTIN)));
      offeredToHisGroup.put(module, reportUserTask(module, fullText, workflowId, """
          "candidateGroups": [ "%s" ]
          """.formatted(GROUP_OF_MARTIN)));
    }

  }

  @Test
  void theListOfAllTasksHoldsOnlyTheTasksOfTheGivenModules() {

    assertThat(taskIds("", moduleIds(moduleA)))
        .containsExactlyInAnyOrder(assignedToMartin.get(moduleA), offeredToHisGroup.get(moduleA));
    assertThat(taskIds("", moduleIds(moduleB)))
        .containsExactlyInAnyOrder(assignedToMartin.get(moduleB), offeredToHisGroup.get(moduleB));
    assertThat(taskIds("", moduleIds(moduleA, moduleB)))
        .containsExactlyInAnyOrderElementsOf(allTasks());

  }

  @Test
  void theListOfTheUsersOwnTasksHoldsOnlyTheTasksOfTheGivenModules() {

    assertThat(taskIds("/current-user", moduleIds(moduleA)))
        .containsExactly(assignedToMartin.get(moduleA));
    assertThat(taskIds("/current-user", ""))
        .containsExactlyInAnyOrder(assignedToMartin.get(moduleA), assignedToMartin.get(moduleB));

  }

  @Test
  void theListOfTheTasksOfTheUsersGroupsHoldsOnlyTheTasksOfTheGivenModules() {

    assertThat(taskIds("/user-roles", moduleIds(moduleB)))
        .containsExactly(offeredToHisGroup.get(moduleB));
    assertThat(taskIds("/user-roles", ""))
        .containsExactlyInAnyOrder(offeredToHisGroup.get(moduleA), offeredToHisGroup.get(moduleB));

  }

  @Test
  void aTaskListWithoutTheLimitHoldsTheTasksOfEveryModule() {

    assertThat(taskIds("", ""))
        .as("no workflowModuleIds")
        .containsExactlyInAnyOrderElementsOf(allTasks());
    assertThat(taskIds("", ", \"workflowModuleIds\": []"))
        .as("an empty array")
        .containsExactlyInAnyOrderElementsOf(allTasks());
    assertThat(taskIds("", ", \"workflowModuleIds\": null"))
        .as("null")
        .containsExactlyInAnyOrderElementsOf(allTasks());

  }

  @Test
  void theLimitNeverShowsATaskTheUserMayNotSee() {

    final var petra = loginToGui(USER_PETRA);

    final var response = guiPost(petra, "/usertask", listRequest(moduleIds(moduleA, moduleB)));

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(json(response).read("$.userTasks[*].id", List.class)).isEmpty();

  }

  @Test
  void theUpdateOfATaskListKeepsTheLimit() {

    final var response = guiPut(martin, "/usertask", """
        {
          "size": 50,
          "knownUserTasksIds": [],
          "searchQueries": [ { "query": "%s" } ],
          "mode": "All"
          %s
        }
        """.formatted(token, moduleIds(moduleA)));

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(json(response).read("$.userTasks[*].id", List.class))
        .containsExactlyInAnyOrder(assignedToMartin.get(moduleA), offeredToHisGroup.get(moduleA));

  }

  @Test
  void theCountOfATaskListKeepsTheLimit() {

    final var response = guiPost(martin, "/usertask/count", """
        {
          "filters": [
            { "key": "limited", "mode": "All", "searchQueries": [ { "query": "%s" } ] %s },
            { "key": "every", "mode": "All", "searchQueries": [ { "query": "%s" } ] }
          ]
        }
        """.formatted(token, moduleIds(moduleA), token));

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(countOf(json(response), "limited")).isEqualTo(2);
    assertThat(countOf(json(response), "every")).isEqualTo(4);

  }

  @Test
  void theSuggestionsForATaskListKeepTheLimit() {

    assertThat(suggestions("/usertask", moduleIds(moduleA))).containsExactly(wordOfModuleA);
    assertThat(suggestions("/usertask", ""))
        .containsExactlyInAnyOrder(wordOfModuleA, wordOfModuleB);

  }

  @Test
  void theListOfWorkflowsHoldsOnlyTheWorkflowsOfTheGivenModules() {

    assertThat(workflowIds(moduleIds(moduleA))).containsExactly(workflows.get(moduleA));
    assertThat(workflowIds(moduleIds(moduleA, moduleB)))
        .containsExactlyInAnyOrder(workflows.get(moduleA), workflows.get(moduleB));

  }

  @Test
  void aWorkflowListWithoutTheLimitHoldsTheWorkflowsOfEveryModule() {

    assertThat(workflowIds(""))
        .containsExactlyInAnyOrder(workflows.get(moduleA), workflows.get(moduleB));
    assertThat(workflowIds(", \"workflowModuleIds\": []"))
        .containsExactlyInAnyOrder(workflows.get(moduleA), workflows.get(moduleB));

  }

  @Test
  void theUpdateOfAWorkflowListKeepsTheLimit() {

    final var response = guiPut(martin, "/workflow", """
        {
          "size": 50,
          "knownWorkflowsIds": [],
          "searchQueries": [ { "query": "%s" } ],
          "mode": "All"
          %s
        }
        """.formatted(token, moduleIds(moduleB)));

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(json(response).read("$.workflows[*].id", List.class))
        .containsExactly(workflows.get(moduleB));

  }

  @Test
  void theCountOfAWorkflowListKeepsTheLimit() {

    final var response = guiPost(martin, "/workflow/count", """
        {
          "filters": [
            { "key": "limited", "mode": "All", "searchQueries": [ { "query": "%s" } ] %s },
            { "key": "every", "mode": "All", "searchQueries": [ { "query": "%s" } ] }
          ]
        }
        """.formatted(token, moduleIds(moduleB), token));

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(countOf(json(response), "limited")).isEqualTo(1);
    assertThat(countOf(json(response), "every")).isEqualTo(2);

  }

  @Test
  void theSuggestionsForAWorkflowListKeepTheLimit() {

    assertThat(suggestions("/workflow", moduleIds(moduleB))).containsExactly(wordOfModuleB);
    assertThat(suggestions("/workflow", ""))
        .containsExactlyInAnyOrder(wordOfModuleA, wordOfModuleB);

  }

  private List<String> allTasks() {

    return List.of(
        assignedToMartin.get(moduleA), offeredToHisGroup.get(moduleA),
        assignedToMartin.get(moduleB), offeredToHisGroup.get(moduleB));

  }

  /** The limit as the last property of a request, with the comma in front of it. */
  private static String moduleIds(
      final String... ids) {

    return ", \"workflowModuleIds\": [ "
        + String.join(", ", List.of(ids).stream().map("\"%s\""::formatted).toList())
        + " ]";

  }

  private String listRequest(
      final String limit) {

    return """
        {
          "pageNumber": 0,
          "pageSize": 50,
          "searchQueries": [ { "query": "%s" } ],
          "mode": "All"
          %s
        }
        """.formatted(token, limit);

  }

  private List<String> taskIds(
      final String basePath,
      final String limit) {

    final var response = guiPost(martin, basePath
        + "/usertask", listRequest(limit));
    assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    return json(response).read("$.userTasks[*].id", List.class);

  }

  private List<String> workflowIds(
      final String limit) {

    final var response = guiPost(martin, "/workflow", listRequest(limit));
    assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    return json(response).read("$.workflows[*].id", List.class);

  }

  private static long countOf(
      final DocumentContext counts,
      final String key) {

    return counts
        .<List<Number>>read("$.counts[?(@.key == '"
            + key
            + "')].count")
        .get(0)
        .longValue();

  }

  /**
   * The suggestions of a search field. The request is an {@code OPTIONS} with a body, the way
   * the user interface asks for them.
   */
  private List<String> suggestions(
      final String listPath,
      final String limit) {

    final var response = send(HttpRequest
        .newBuilder(URI.create(url("/gui/api/v1"
            + listPath
            + "?path=detailsFulltextSearch&query="
            + URLEncoder.encode(typed, StandardCharsets.UTF_8))))
        .header("Cookie", martin)
        .header("Content-Type", "application/json")
        .method("OPTIONS", HttpRequest.BodyPublishers.ofString("""
            {
              "searchQueries": [ { "query": "%s" } ]
              %s
            }
            """.formatted(token, limit)))
        .build());
    assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    return json(response).read("$.result[*].item", List.class);

  }

}
