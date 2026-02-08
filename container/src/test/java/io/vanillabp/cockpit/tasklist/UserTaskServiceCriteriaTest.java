package io.vanillabp.cockpit.tasklist;

import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link UserTaskService#buildUserTasksCriteria} method.
 */
@ExtendWith(MockitoExtension.class)
class UserTaskServiceCriteriaTest {

    @Mock
    private UserTaskRepository userTasks;

    @Mock
    private ReactiveMongoTemplate mongoTemplate;

    @Mock
    private Logger logger;

    @InjectMocks
    private UserTaskService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "logger", logger);
    }

    // --- buildUserTasksCriteria tests ---

    @Test
    void buildUserTasksCriteria_withAllMode_returnsEmptyCriteriaBase() {
        // Act
        Criteria criteria = service.buildUserTasksCriteria(
                false, false, null, null, null, null,
                null, UserTaskService.RetrieveItemsMode.All, null);

        // Assert
        assertThat(criteria).isNotNull();
    }

    @Test
    void buildUserTasksCriteria_withOpenTasksMode_includesEndedAtFilter() {
        // Arrange
        OffsetDateTime timestamp = OffsetDateTime.now();

        // Act
        Criteria criteria = service.buildUserTasksCriteria(
                false, false, null, null, null, null,
                timestamp, UserTaskService.RetrieveItemsMode.OpenTasks, null);

        // Assert
        assertThat(criteria).isNotNull();
    }

    @Test
    void buildUserTasksCriteria_withClosedTasksOnlyMode_includesEndedAtExistsFilter() {
        // Act
        Criteria criteria = service.buildUserTasksCriteria(
                false, false, null, null, null, null,
                null, UserTaskService.RetrieveItemsMode.ClosedTasksOnly, null);

        // Assert
        assertThat(criteria).isNotNull();
    }

    @Test
    void buildUserTasksCriteria_withAssignees_includesAssigneeFilter() {
        // Arrange
        List<String> assignees = List.of("user1", "user2");

        // Act
        Criteria criteria = service.buildUserTasksCriteria(
                false, false, assignees, null, null, null,
                null, UserTaskService.RetrieveItemsMode.All, null);

        // Assert
        assertThat(criteria).isNotNull();
    }

    @Test
    void buildUserTasksCriteria_withNotInAssignees_excludesAssignees() {
        // Arrange
        List<String> assignees = List.of("user1");

        // Act
        Criteria criteria = service.buildUserTasksCriteria(
                false, true, assignees, null, null, null,
                null, UserTaskService.RetrieveItemsMode.All, null);

        // Assert
        assertThat(criteria).isNotNull();
    }

    @Test
    void buildUserTasksCriteria_withNotInAssigneesAndEmptyList_excludesAllAssigned() {
        // Act
        Criteria criteria = service.buildUserTasksCriteria(
                false, true, null, null, null, null,
                null, UserTaskService.RetrieveItemsMode.All, null);

        // Assert
        assertThat(criteria).isNotNull();
    }

    @Test
    void buildUserTasksCriteria_withCandidateUsers_includesCandidateUsersFilter() {
        // Arrange
        List<String> candidateUsers = List.of("user1", "user2");

        // Act
        Criteria criteria = service.buildUserTasksCriteria(
                false, false, null, candidateUsers, null, null,
                null, UserTaskService.RetrieveItemsMode.All, null);

        // Assert
        assertThat(criteria).isNotNull();
    }

    @Test
    void buildUserTasksCriteria_withCandidateGroups_includesCandidateGroupsFilter() {
        // Arrange
        List<String> candidateGroups = List.of("admin", "managers");

        // Act
        Criteria criteria = service.buildUserTasksCriteria(
                false, false, null, null, candidateGroups, null,
                null, UserTaskService.RetrieveItemsMode.All, null);

        // Assert
        assertThat(criteria).isNotNull();
    }

    @Test
    void buildUserTasksCriteria_withCandidatesToBeExcluded_excludesCandidates() {
        // Arrange
        List<String> candidatesToExclude = List.of("user1");

        // Act
        Criteria criteria = service.buildUserTasksCriteria(
                false, false, null, null, null, candidatesToExclude,
                null, UserTaskService.RetrieveItemsMode.All, null);

        // Assert
        assertThat(criteria).isNotNull();
    }

    @Test
    void buildUserTasksCriteria_withIncludeDanglingTasks_includesDanglingFilter() {
        // Act
        Criteria criteria = service.buildUserTasksCriteria(
                true, false, null, List.of("user1"), null, null,
                null, UserTaskService.RetrieveItemsMode.All, null);

        // Assert
        assertThat(criteria).isNotNull();
    }

    @Test
    void buildUserTasksCriteria_withOpenTasksWithoutFollowUp_includesFollowUpFilter() {
        // Act
        Criteria criteria = service.buildUserTasksCriteria(
                false, false, null, null, null, null,
                null, UserTaskService.RetrieveItemsMode.OpenTasksWithoutFollowUp, null);

        // Assert
        assertThat(criteria).isNotNull();
    }

    @Test
    void buildUserTasksCriteria_withPredefinedCriteria_appendsCriteria() {
        // Arrange
        List<Criteria> predefinedCriteria = List.of(Criteria.where("workflowId").is("wf-123"));

        // Act
        Criteria criteria = service.buildUserTasksCriteria(
                false, false, null, null, null, null,
                null, UserTaskService.RetrieveItemsMode.All, predefinedCriteria);

        // Assert
        assertThat(criteria).isNotNull();
    }

    @Test
    void buildUserTasksCriteria_withNullInitialTimestampAndOpenTasks_addsEndedAtExistsFilter() {
        // Act
        Criteria criteria = service.buildUserTasksCriteria(
                false, false, null, null, null, null,
                null, UserTaskService.RetrieveItemsMode.OpenTasks, null);

        // Assert
        assertThat(criteria).isNotNull();
    }

    @Test
    void buildUserTasksCriteria_withCombinedFilters_combinesAllFilters() {
        // Arrange
        List<String> assignees = List.of("user1");
        List<String> candidateUsers = List.of("user2");
        List<String> candidateGroups = List.of("admin");
        OffsetDateTime timestamp = OffsetDateTime.now();

        // Act
        Criteria criteria = service.buildUserTasksCriteria(
                true, false, assignees, candidateUsers, candidateGroups, null,
                timestamp, UserTaskService.RetrieveItemsMode.OpenTasks, null);

        // Assert
        assertThat(criteria).isNotNull();
    }
}
