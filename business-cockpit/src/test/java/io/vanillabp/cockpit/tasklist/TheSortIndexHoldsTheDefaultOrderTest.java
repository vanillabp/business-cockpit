package io.vanillabp.cockpit.tasklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import io.vanillabp.cockpit.exceptions.BcInvalidRequestException;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.util.ListPaths;
import io.vanillabp.cockpit.util.SortIndexes;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Both lists add their default order behind the paths a request sorts by, so paging stays stable:
 * user tasks {@code dueDate, createdAt, _id}, workflows {@code createdAt, _id}. The index of a sort
 * covers the requested paths and these fields, in this order. A sort which names two fields that
 * each hold a list is refused before any index is built.
 */
@ExtendWith(SuppressOutputExtension.class)
class TheSortIndexHoldsTheDefaultOrderTest {

  private static UserTaskService userTaskService() {

    final var service = new UserTaskService();
    ReflectionTestUtils.setField(service, "sortIndexes",
        new SortIndexes(UserTask.COLLECTION_NAME, new ListPaths(UserTaskService.FIELDS_OF_THE_LIST, Optional
            .empty()), UserTaskService.LIST_FIELDS_OF_THE_LIST, 30, "limit", mock(
                MongoTemplate.class), mock(Logger.class)));
    return service;

  }

  private static WorkflowlistService workflowlistService() {

    final var service = new WorkflowlistService();
    ReflectionTestUtils.setField(service, "sortIndexes",
        new SortIndexes(Workflow.COLLECTION_NAME, new ListPaths(WorkflowlistService.FIELDS_OF_THE_LIST, Optional
            .empty()), WorkflowlistService.LIST_FIELDS_OF_THE_LIST, 30, "limit", mock(
                MongoTemplate.class), mock(Logger.class)));
    return service;

  }

  private static List<String> indexedFields(
      final Object order) {

    return ReflectionTestUtils.invokeMethod(order, "toBeIndexed");

  }

  @Test
  void theIndexOfAColumnOfPersonsHoldsTheDefaultOrderOfTheTasks() {

    final Object order = ReflectionTestUtils.invokeMethod(
        userTaskService(), "getUserTaskListOrder", "candidateUsers.sort,candidateUsers.id", true);

    assertThat(indexedFields(order))
        .containsExactly("candidateUsers.sort", "candidateUsers.id", "dueDate", "createdAt", "_id");

  }

  @Test
  void theIndexOfAWorkflowSortHoldsTheDefaultOrderOfTheWorkflows() {

    final Object order = ReflectionTestUtils.invokeMethod(
        workflowlistService(), "getWorkflowListOrder", "accessibleToUsers.sort,accessibleToUsers.id", true);

    assertThat(indexedFields(order))
        .containsExactly("accessibleToUsers.sort", "accessibleToUsers.id", "createdAt", "_id");

  }

  @Test
  void aSortOfTheCandidateUsersAndTheCandidateGroupsIsRefused() {

    assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
        userTaskService(), "getUserTaskListOrder", "candidateUsers.sort,candidateGroups.id", true))
        .isInstanceOf(BcInvalidRequestException.class)
        .hasMessage("'sort' may name only one of the fields 'candidateGroups' and 'candidateUsers' at a time, "
            + "because each of them holds a list");

  }

  @Test
  void aSortOfBothAccessListsOfAWorkflowIsRefused() {

    assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
        workflowlistService(), "getWorkflowListOrder", "accessibleToGroups.id,accessibleToUsers.sort", true))
        .isInstanceOf(BcInvalidRequestException.class)
        .hasMessage("'sort' may name only one of the fields 'accessibleToGroups' and 'accessibleToUsers' at a "
            + "time, because each of them holds a list");

  }

}
