package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;

import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The count endpoints count with the conditions of their list. For every mode, with and without
 * filters, for two users who see different tasks, and on every base path of the task list, the
 * count is the {@code page.totalElements} of the list. Counting creates no index, because it does
 * not sort.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class ACountAgreesWithItsListTest extends ItestBase {

  private static final List<String> TASK_MODES = List.of(
      "All", "OpenTasksWithFollowUp", "OpenTasksWithoutFollowUp", "OpenTaskOnlyFollowUp",
      "ClosedTasksOnly");

  private static final List<String> WORKFLOW_MODES = List.of("All", "Active", "Inactive");

  @Autowired
  private MongoTemplate mongo;

  private String moduleId;
  private String token;
  private String martin;
  private String petra;

  @BeforeEach
  void reportTasksForMartinAndPetra() {

    moduleId = unique("count-module");
    token = unique("token");
    registerWorkflowModule(moduleId, "http://localhost:65000");
    martin = loginToGui(USER_MARTIN);
    petra = loginToGui(USER_PETRA);

    final var workflowId = unique("workflow");
    reportUserTask(moduleId, token, workflowId, "\"assignee\": \"martin\"");
    reportUserTask(moduleId, token, workflowId, "\"candidateGroups\": [ \"accounting\" ]");
    final var followedUp = reportUserTask(moduleId, token, workflowId, "\"assignee\": \"martin\"");
    assertThat(guiPatch(martin, "/usertask/"
        + followedUp
        + "/follow-up-date", """
            { "timestamp": "%s" }
            """.formatted(OffsetDateTime.now().plusDays(3))).statusCode()).isEqualTo(200);
    reportUserTaskCompleted(moduleId, token,
        reportUserTask(moduleId, token, workflowId, "\"assignee\": \"martin\""));
    reportUserTask(moduleId, token, workflowId, "\"candidateUsers\": [ \"petra\" ]");
    reportUserTaskCompleted(moduleId, token,
        reportUserTask(moduleId, token, workflowId, "\"candidateGroups\": [ \"sales\" ]"));

  }

  @Test
  void theCountOfEveryModeIsTheTotalOfItsList() {

    for (final var cookie : List.of(martin, petra)) {
      for (final var basePath : List.of("", "/current-user", "/user-roles")) {
        for (final var searchQueries : List.of("", """
            , "searchQueries": [ { "query": "%s" } ]""".formatted(token))) {
          final var modes = new ArrayList<String>(TASK_MODES.stream()
              .map("\"mode\": \"%s\","::formatted)
              .toList());
          modes.add("");
          final var counts = countsOf(guiPost(cookie, basePath
              + "/usertask/count", filtersOf(modes, searchQueries)));
          for (var position = 0; position < modes.size(); ++position) {
            final var list = guiPost(cookie, basePath
                + "/usertask", "{ %s \"pageSize\": 1%s }".formatted(modes.get(position), searchQueries));
            assertThat(list.statusCode()).isEqualTo(200);
            assertThat(counts.get(position))
                .as("%s %s %s", basePath, modes.get(position), searchQueries)
                .isEqualTo(json(list).read("$.page.totalElements", Long.class));
          }
        }
      }
    }

  }

  @Test
  void theCountsOfTwoUsersFollowWhatEachOfThemSees() {

    final var filters = filtersOf(
        TASK_MODES.stream().map("\"mode\": \"%s\","::formatted).toList(),
        ", \"searchQueries\": [ { \"query\": \"%s\" } ]".formatted(token));

    // martin: his own two open tasks, one of them on follow-up, his group's task, his ended task
    assertThat(countsOf(guiPost(martin, "/usertask/count", filters)))
        .containsExactly(4L, 3L, 2L, 1L, 1L);
    // petra: the task naming her, and the ended task of her group
    assertThat(countsOf(guiPost(petra, "/usertask/count", filters)))
        .containsExactly(2L, 1L, 1L, 0L, 1L);

  }

  @Test
  void theCountOfEveryModeOfTheWorkflowsIsTheTotalOfItsList() {

    final var running = unique("workflow");
    final var ended = unique("workflow");
    reportWorkflow(moduleId, token, running, "created");
    reportWorkflow(moduleId, token, ended, "created");
    reportWorkflow(moduleId, token, ended, "completed");

    for (final var filter : List.of("", """
        , "searchQueries": [ { "query": "%s" } ]""".formatted(token), """
        , "businessIds": [ "no-such-business-id" ]""")) {
      final var modes = new ArrayList<String>(WORKFLOW_MODES.stream()
          .map("\"mode\": \"%s\","::formatted)
          .toList());
      modes.add("");
      final var counts = countsOf(guiPost(martin, "/workflow/count", filtersOf(modes, filter)));
      for (var position = 0; position < modes.size(); ++position) {
        final var list = guiPost(martin, "/workflow", "{ %s \"pageSize\": 1%s }"
            .formatted(modes.get(position), filter));
        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(counts.get(position))
            .as("%s %s", modes.get(position), filter)
            .isEqualTo(json(list).read("$.page.totalElements", Long.class));
      }
    }

    assertThat(countsOf(guiPost(martin, "/workflow/count", filtersOf(
        WORKFLOW_MODES.stream().map("\"mode\": \"%s\","::formatted).toList(),
        ", \"searchQueries\": [ { \"query\": \"%s\" } ]".formatted(token)))))
        .containsExactly(2L, 1L, 1L);

  }

  @Test
  void countingCreatesNoIndex() {

    final var tasksBefore = indexesOf(UserTask.COLLECTION_NAME);
    final var workflowsBefore = indexesOf(Workflow.COLLECTION_NAME);

    assertThat(guiPost(martin, "/usertask/count", filtersOf(
        TASK_MODES.stream().map("\"mode\": \"%s\","::formatted).toList(), "")).statusCode())
        .isEqualTo(200);
    assertThat(guiPost(martin, "/workflow/count", filtersOf(
        WORKFLOW_MODES.stream().map("\"mode\": \"%s\","::formatted).toList(), "")).statusCode())
        .isEqualTo(200);

    assertThat(indexesOf(UserTask.COLLECTION_NAME)).isEqualTo(tasksBefore);
    assertThat(indexesOf(Workflow.COLLECTION_NAME)).isEqualTo(workflowsBefore);

  }

  @Test
  void aKeyUsedTwiceIsAnsweredWith400WhichNamesThePosition() {

    final var response = guiPost(martin, "/usertask/count", """
        { "filters": [ { "key": "open" }, { "key": "closed" }, { "key": "open" } ] }
        """);

    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(response.body()).isEqualTo("The request is not valid: 'filters[2].key' is used by "
        + "an earlier filter already.");

  }

  @Test
  void aRequestWithoutFiltersOrWithTooManyIsAnsweredWith400() {

    assertThat(guiPost(martin, "/usertask/count", "{ \"filters\": [] }").statusCode())
        .isEqualTo(400);
    assertThat(guiPost(martin, "/workflow/count", "{ \"filters\": [] }").statusCode())
        .isEqualTo(400);
    final var tooMany = IntStream
        .range(0, 21)
        .mapToObj("{ \"key\": \"f%d\" }"::formatted)
        .collect(Collectors.joining(", ", "{ \"filters\": [ ", " ] }"));
    assertThat(guiPost(martin, "/usertask/count", tooMany).statusCode()).isEqualTo(400);

  }

  /**
   * @param modes The start of each filter, like {@code "mode": "All",}, or an empty string
   * @param rest The rest of each filter, starting with a comma, or an empty string
   * @return A count request with one filter per mode, keyed by its position
   */
  private static String filtersOf(
      final List<String> modes,
      final String rest) {

    return IntStream
        .range(0, modes.size())
        .mapToObj(position -> "{ %s \"key\": \"filter-%d\"%s }"
            .formatted(modes.get(position), position, rest))
        .collect(Collectors.joining(", ", "{ \"filters\": [ ", " ] }"));

  }

  /** The counts of an answer, after checking that each carries the key of its position. */
  @SuppressWarnings("unchecked")
  private static List<Long> countsOf(
      final java.net.http.HttpResponse<String> response) {

    assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    final var answer = json(response);
    final var keys = answer.read("$.counts[*].key", List.class);
    for (var position = 0; position < keys.size(); ++position) {
      assertThat(keys.get(position)).isEqualTo("filter-"
          + position);
    }
    return ((List<Number>) answer.read("$.counts[*].count", List.class))
        .stream()
        .map(Number::longValue)
        .toList();

  }

  private List<String> indexesOf(
      final String collection) {

    return mongo
        .indexOps(collection)
        .getIndexInfo()
        .stream()
        .map(IndexInfo::getName)
        .sorted()
        .toList();

  }

}
