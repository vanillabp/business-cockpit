package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.util.SortIndexes;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What the two lists of the GUI API do with the paths in {@code sort}. Each new combination of
 * paths used to get an index of its own, without a check and without a limit, until MongoDB
 * refused any more at 64 indexes per collection. Now a path names a field of the list or a key of
 * the business data, and at most 30 sort indexes are created per collection, the default of
 * {@code business-cockpit.mongodb.sort-indexes-per-collection}.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class SortPathsTest extends ItestBase {

  @Autowired
  private MongoTemplate mongo;

  private String cookie;

  @BeforeEach
  void login() {
    cookie = loginToGui(USER_MARTIN);
  }

  private int listSortedBy(
      final String list,
      final String sort) {

    return guiPost(cookie, "/"
        + list, """
            { "pageNumber": 0, "pageSize": 1, "sort": "%s" }
            """.formatted(sort)).statusCode();

  }

  private long sortIndexesOf(
      final String collection) {

    return mongo
        .indexOps(collection)
        .getIndexInfo()
        .stream()
        .filter(index -> index.getName().startsWith(SortIndexes.INDEX_PREFIX))
        .count();

  }

  @Test
  void theTaskListIsSortedByItsFieldsAndByBusinessData() {

    assertThat(listSortedBy("usertask", "title.en")).isEqualTo(200);
    assertThat(listSortedBy("usertask", "assignee.sort,assignee.id")).isEqualTo(200);
    assertThat(listSortedBy("usertask", "details.customer.name")).isEqualTo(200);
    assertThat(listSortedBy("usertask", "details.order-id")).isEqualTo(200);

  }

  @Test
  void theCaseListIsSortedByItsFieldsAndByBusinessData() {

    assertThat(listSortedBy("workflow", "title.en")).isEqualTo(200);
    assertThat(listSortedBy("workflow", "businessId")).isEqualTo(200);
    assertThat(listSortedBy("workflow", "details.customer.name")).isEqualTo(200);

  }

  @Test
  void aPathWhichIsNoFieldOfTheTaskListIsRefusedAndNoIndexIsCreated() {

    final var before = sortIndexesOf(UserTask.COLLECTION_NAME);

    final var answer = guiPost(cookie, "/usertask", """
        { "pageNumber": 0, "pageSize": 1, "sort": "dueDate,noSuchField" }
        """);

    assertThat(answer.statusCode()).isEqualTo(400);
    assertThat(answer.body()).isEqualTo("The request is not valid: 'sort' may only name fields of "
        + "the list or keys below 'details.'. A part of a path holds letters, digits and '_', "
        + "and below 'details.' also '-'.");
    assertThat(sortIndexesOf(UserTask.COLLECTION_NAME)).isEqualTo(before);

  }

  @Test
  void aPathWhichIsNoFieldOfTheCaseListIsRefused() {

    assertThat(listSortedBy("workflow", "dueDate")).isEqualTo(400);
    assertThat(listSortedBy("workflow", "details.$where")).isEqualTo(400);

  }

  @Test
  void anUpdateOfAListChecksItsPathsAsWell() {

    final var answer = send(java.net.http.HttpRequest
        .newBuilder(java.net.URI.create(url("/gui/api/v1/usertask")))
        .header("Cookie", cookie)
        .header("Content-Type", "application/json")
        .PUT(java.net.http.HttpRequest.BodyPublishers.ofString("""
            { "size": 1, "knownUserTasksIds": [], "sort": "noSuchField" }
            """))
        .build());

    assertThat(answer.statusCode()).isEqualTo(400);

  }

  /**
   * A sort names at most five paths in at most 500 characters. A longer one is refused before
   * any of its paths is looked at, and the answer does not repeat it.
   */
  @Test
  void aSortOfMoreThan500CharactersIsRefused() {

    final var tooLong = "details.a"
        + "a".repeat(492);

    assertThat(listSortedBy("usertask", tooLong)).isEqualTo(400);
    assertThat(listSortedBy("workflow", tooLong)).isEqualTo(400);
    assertThat(guiPost(cookie, "/usertask", """
        { "pageNumber": 0, "pageSize": 1, "sort": "%s" }
        """.formatted(tooLong)).body()).doesNotContain(tooLong);

  }

  @Test
  void aSortOfMoreThanFivePathsIsRefused() {

    assertThat(listSortedBy("usertask", "details.a,details.b,details.c,details.d,details.e,details.f"))
        .isEqualTo(400);
    assertThat(listSortedBy("workflow", "details.a,details.b,details.c,details.d,details.e,details.f"))
        .isEqualTo(400);

  }

  /** The positive twin: five paths in 500 characters are sorted by. */
  @Test
  void aSortOfFivePathsIn500CharactersIsSortedBy() {

    final var fivePaths = "details.b,details.c,details.d,details.e,details.a"
        + "a".repeat(451);
    assertThat(fivePaths).hasSize(500);

    assertThat(listSortedBy("usertask", fivePaths)).isEqualTo(200);
    assertThat(listSortedBy("workflow", fivePaths)).isEqualTo(200);

  }

  /**
   * A hundred paths below {@code details} are a hundred allowed combinations, and the lists are
   * sorted by each of them. The collections get no more than 30 sort indexes all the same.
   */
  @Test
  void aHundredNewPathsCreateNoMoreIndexesThanTheLimit() {

    for (int i = 0; i < 100; ++i) {
      assertThat(listSortedBy("usertask", "details.limit"
          + i)).isEqualTo(200);
      assertThat(listSortedBy("workflow", "details.limit"
          + i)).isEqualTo(200);
    }

    assertThat(sortIndexesOf(UserTask.COLLECTION_NAME)).isEqualTo(30);
    assertThat(sortIndexesOf(Workflow.COLLECTION_NAME)).isEqualTo(30);

  }

}
