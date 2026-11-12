package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A list of workflows filtered by business ids shows the same workflows after an update as on its
 * first page. The browser asks for the update after each event of the update stream. If the update
 * left out the business ids, the list would suddenly show workflows which do not belong to it.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class TheUpdateOfAListFiltersLikeItsPageTest extends ItestBase {

  private String moduleId;
  private String token;
  private String cookie;

  @BeforeEach
  void registerModuleAndLogin() {
    moduleId = unique("ride-module");
    token = unique("token");
    registerWorkflowModule(moduleId, "http://localhost:65000");
    cookie = loginToGui(USER_MARTIN);
  }

  @Test
  void theUpdateOfAListOfWorkflowsKeepsToTheBusinessIdsOfThePage() {

    final var wanted = unique("workflow");
    final var other = unique("workflow");
    final var withoutBusinessId = unique("workflow");
    final var wantedBusinessId = unique("order");
    reportWorkflow(moduleId, token, wanted, "created", wantedBusinessId);
    reportWorkflow(moduleId, token, other, "created", unique("order"));
    reportWorkflow(moduleId, token, withoutBusinessId, "created");

    final var filter = """
        "searchQueries": [ { "query": "%s" } ], "businessIds": [ "%s" ]
        """.formatted(token, wantedBusinessId);

    final var firstPage = guiPost(cookie, "/workflow", "{ \"pageSize\": 50, %s }".formatted(filter));
    assertThat(firstPage.statusCode()).as(firstPage.body()).isEqualTo(200);
    assertThat(json(firstPage).read("$.workflows[*].id", List.class)).containsExactly(wanted);

    final var update = guiPut(cookie, "/workflow", "{ \"size\": 50, \"knownWorkflowsIds\": [], %s }"
        .formatted(filter));
    assertThat(update.statusCode()).as(update.body()).isEqualTo(200);
    assertThat(json(update).read("$.workflows[*].id", List.class)).containsExactly(wanted);
    assertThat(json(update).read("$.page.totalElements", Long.class)).isEqualTo(1L);

  }

  @Test
  void anUpdateWithoutBusinessIdsShowsEveryWorkflowOfTheList() {

    final var first = unique("workflow");
    final var second = unique("workflow");
    reportWorkflow(moduleId, token, first, "created", unique("order"));
    reportWorkflow(moduleId, token, second, "created", unique("order"));

    final var update = guiPut(cookie, "/workflow", """
        { "size": 50, "knownWorkflowsIds": [], "searchQueries": [ { "query": "%s" } ] }
        """.formatted(token));
    assertThat(update.statusCode()).as(update.body()).isEqualTo(200);
    assertThat(json(update).read("$.workflows[*].id", List.class))
        .containsExactlyInAnyOrder(first, second);

  }

}
