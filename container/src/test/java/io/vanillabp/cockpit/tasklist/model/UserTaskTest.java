package io.vanillabp.cockpit.tasklist.model;

import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link UserTask}.
 */
class UserTaskTest {

    private UserTask userTask;

    @BeforeEach
    void setUp() {
        userTask = new UserTask();
    }

    // --- isDangling tests ---

    @Test
    void isDangling_withNoCandidatesOrAssignee_returnsTrue() {
        // Arrange - no candidates or assignee set

        // Act & Assert
        assertThat(userTask.isDangling()).isTrue();
    }

    @Test
    void isDangling_withCandidateUsers_returnsFalse() {
        // Arrange
        Person person = createPerson("user1");
        userTask.setCandidateUsers(new ArrayList<>(List.of(person)));

        // Act & Assert
        assertThat(userTask.isDangling()).isFalse();
    }

    @Test
    void isDangling_withCandidateGroups_returnsFalse() {
        // Arrange
        Group group = createGroup("group1");
        userTask.setCandidateGroups(new ArrayList<>(List.of(group)));

        // Act & Assert
        assertThat(userTask.isDangling()).isFalse();
    }

    @Test
    void isDangling_withAssignee_returnsFalse() {
        // Arrange
        Person assignee = createPerson("assignee");
        userTask.setAssignee(assignee);

        // Act & Assert
        assertThat(userTask.isDangling()).isFalse();
    }

    @Test
    void isDangling_withEmptyCandidateLists_returnsTrue() {
        // Arrange
        userTask.setCandidateUsers(new ArrayList<>());
        userTask.setCandidateGroups(new ArrayList<>());

        // Act & Assert
        assertThat(userTask.isDangling()).isTrue();
    }

    // --- addCandidatePerson tests ---

    @Test
    void addCandidatePerson_withNullPerson_doesNothing() {
        // Arrange - empty task

        // Act
        userTask.addCandidatePerson(null);

        // Assert
        assertThat(userTask.getCandidateUsers()).isNull();
    }

    @Test
    void addCandidatePerson_withNoCandidates_createsListWithPerson() {
        // Arrange
        Person person = createPerson("user1");

        // Act
        userTask.addCandidatePerson(person);

        // Assert
        assertThat(userTask.getCandidateUsers()).hasSize(1);
        assertThat(userTask.getCandidateUsers().get(0).getId()).isEqualTo("user1");
    }

    @Test
    void addCandidatePerson_withExistingCandidates_addsPerson() {
        // Arrange
        Person existingPerson = createPerson("user1");
        userTask.setCandidateUsers(new ArrayList<>(List.of(existingPerson)));
        Person newPerson = createPerson("user2");

        // Act
        userTask.addCandidatePerson(newPerson);

        // Assert
        assertThat(userTask.getCandidateUsers()).hasSize(2);
    }

    @Test
    void addCandidatePerson_withDuplicatePerson_replacesPerson() {
        // Arrange
        Person person1 = createPerson("user1");
        person1.setFulltext("Original");
        userTask.setCandidateUsers(new ArrayList<>(List.of(person1)));

        Person updatedPerson = createPerson("user1");
        updatedPerson.setFulltext("Updated");

        // Act
        userTask.addCandidatePerson(updatedPerson);

        // Assert - should still have only one person with updated data
        assertThat(userTask.getCandidateUsers()).hasSize(1);
        assertThat(userTask.getCandidateUsers().get(0).getFulltext()).isEqualTo("Updated");
    }

    // --- removeCandidatePerson tests ---

    @Test
    void removeCandidatePerson_withNullPersonId_doesNothing() {
        // Arrange
        Person person = createPerson("user1");
        userTask.setCandidateUsers(new ArrayList<>(List.of(person)));

        // Act
        userTask.removeCandidatePerson(null);

        // Assert
        assertThat(userTask.getCandidateUsers()).hasSize(1);
    }

    @Test
    void removeCandidatePerson_withNoCandidates_doesNothing() {
        // Arrange - no candidates

        // Act
        userTask.removeCandidatePerson("user1");

        // Assert
        assertThat(userTask.getCandidateUsers()).isNull();
    }

    @Test
    void removeCandidatePerson_withEmptyCandidates_doesNothing() {
        // Arrange
        userTask.setCandidateUsers(new ArrayList<>());

        // Act
        userTask.removeCandidatePerson("user1");

        // Assert
        assertThat(userTask.getCandidateUsers()).isEmpty();
    }

    @Test
    void removeCandidatePerson_withExistingPerson_removesPerson() {
        // Arrange
        Person person1 = createPerson("user1");
        Person person2 = createPerson("user2");
        userTask.setCandidateUsers(new ArrayList<>(List.of(person1, person2)));

        // Act
        userTask.removeCandidatePerson("user1");

        // Assert
        assertThat(userTask.getCandidateUsers()).hasSize(1);
        assertThat(userTask.getCandidateUsers().get(0).getId()).isEqualTo("user2");
    }

    // --- getReadAt tests ---

    @Test
    void getReadAt_withNullUserId_returnsNull() {
        // Act & Assert
        assertThat(userTask.getReadAt(null)).isNull();
    }

    @Test
    void getReadAt_withNoReadBy_returnsNull() {
        // Act & Assert
        assertThat(userTask.getReadAt("user1")).isNull();
    }

    @Test
    void getReadAt_withUserNotInReadBy_returnsNull() {
        // Arrange
        userTask.setReadBy(new ArrayList<>(List.of(
                new UserTask.ReadBy("other-user", OffsetDateTime.now())
        )));

        // Act & Assert
        assertThat(userTask.getReadAt("user1")).isNull();
    }

    @Test
    void getReadAt_withUserInReadBy_returnsTimestamp() {
        // Arrange
        OffsetDateTime readTime = OffsetDateTime.now();
        userTask.setReadBy(new ArrayList<>(List.of(
                new UserTask.ReadBy("user1", readTime)
        )));

        // Act & Assert
        assertThat(userTask.getReadAt("user1")).isEqualTo(readTime);
    }

    // --- setReadAt tests ---

    @Test
    void setReadAt_withNoReadBy_createsListWithEntry() {
        // Act
        userTask.setReadAt("user1");

        // Assert
        assertThat(userTask.getReadBy()).hasSize(1);
        assertThat(userTask.getReadBy().get(0).userId()).isEqualTo("user1");
        assertThat(userTask.getReadBy().get(0).timestamp()).isNotNull();
    }

    @Test
    void setReadAt_withExistingReadBy_addsEntry() {
        // Arrange
        userTask.setReadBy(new ArrayList<>(List.of(
                new UserTask.ReadBy("user1", OffsetDateTime.now())
        )));

        // Act
        userTask.setReadAt("user2");

        // Assert
        assertThat(userTask.getReadBy()).hasSize(2);
    }

    @Test
    void setReadAt_withDuplicateUser_updatesTimestamp() {
        // Arrange
        OffsetDateTime oldTime = OffsetDateTime.now().minusHours(1);
        userTask.setReadBy(new ArrayList<>(List.of(
                new UserTask.ReadBy("user1", oldTime)
        )));

        // Act
        userTask.setReadAt("user1");

        // Assert
        assertThat(userTask.getReadBy()).hasSize(1);
        assertThat(userTask.getReadBy().get(0).userId()).isEqualTo("user1");
        assertThat(userTask.getReadBy().get(0).timestamp()).isAfter(oldTime);
    }

    // --- clearReadAt tests ---

    @Test
    void clearReadAt_withNullReadBy_doesNothing() {
        // Act
        userTask.clearReadAt("user1");

        // Assert
        assertThat(userTask.getReadBy()).isNull();
    }

    @Test
    void clearReadAt_withUserInReadBy_removesEntry() {
        // Arrange
        userTask.setReadBy(new ArrayList<>(List.of(
                new UserTask.ReadBy("user1", OffsetDateTime.now()),
                new UserTask.ReadBy("user2", OffsetDateTime.now())
        )));

        // Act
        userTask.clearReadAt("user1");

        // Assert
        assertThat(userTask.getReadBy()).hasSize(1);
        assertThat(userTask.getReadBy().get(0).userId()).isEqualTo("user2");
    }

    // --- Basic getter/setter tests ---

    @Test
    void settersAndGetters_workCorrectly() {
        // Arrange
        OffsetDateTime now = OffsetDateTime.now();
        Map<String, String> title = Map.of("en", "Test Task");

        // Act
        userTask.setId("task-123");
        userTask.setVersion(1L);
        userTask.setInitiator("initiator-user");
        userTask.setCreatedAt(now);
        userTask.setUpdatedAt(now);
        userTask.setUpdatedBy("updater");
        userTask.setEndedAt(now);
        userTask.setSource("test-source");
        userTask.setWorkflowModuleId("module-1");
        userTask.setComment("Test comment");
        userTask.setBpmnProcessId("process-1");
        userTask.setBpmnProcessVersion("1.0");
        userTask.setBusinessId("business-123");
        userTask.setWorkflowId("workflow-123");
        userTask.setSubWorkflowId("sub-workflow-123");
        userTask.setTitle(title);
        userTask.setBpmnTaskId("task-def");
        userTask.setTaskDefinition("UserTask_1");
        userTask.setUiUriPath("/tasks/123");
        userTask.setUiUriType(UiUriType.EXTERNAL);
        userTask.setDueDate(now);
        userTask.setFollowUpDate(now);
        userTask.setDetails(Map.of("key", "value"));
        userTask.setDetailsFulltextSearch("fulltext search");

        // Assert
        assertThat(userTask.getId()).isEqualTo("task-123");
        assertThat(userTask.getVersion()).isEqualTo(1L);
        assertThat(userTask.getInitiator()).isEqualTo("initiator-user");
        assertThat(userTask.getCreatedAt()).isEqualTo(now);
        assertThat(userTask.getUpdatedAt()).isEqualTo(now);
        assertThat(userTask.getUpdatedBy()).isEqualTo("updater");
        assertThat(userTask.getEndedAt()).isEqualTo(now);
        assertThat(userTask.getSource()).isEqualTo("test-source");
        assertThat(userTask.getWorkflowModuleId()).isEqualTo("module-1");
        assertThat(userTask.getComment()).isEqualTo("Test comment");
        assertThat(userTask.getBpmnProcessId()).isEqualTo("process-1");
        assertThat(userTask.getBpmnProcessVersion()).isEqualTo("1.0");
        assertThat(userTask.getBusinessId()).isEqualTo("business-123");
        assertThat(userTask.getWorkflowId()).isEqualTo("workflow-123");
        assertThat(userTask.getSubWorkflowId()).isEqualTo("sub-workflow-123");
        assertThat(userTask.getTitle()).isEqualTo(title);
        assertThat(userTask.getBpmnTaskId()).isEqualTo("task-def");
        assertThat(userTask.getTaskDefinition()).isEqualTo("UserTask_1");
        assertThat(userTask.getUiUriPath()).isEqualTo("/tasks/123");
        assertThat(userTask.getUiUriType()).isEqualTo(UiUriType.EXTERNAL);
        assertThat(userTask.getDueDate()).isEqualTo(now);
        assertThat(userTask.getFollowUpDate()).isEqualTo(now);
        assertThat(userTask.getDetails()).containsEntry("key", "value");
        assertThat(userTask.getDetailsFulltextSearch()).isEqualTo("fulltext search");
    }

    @Test
    void setDangling_isIgnored() {
        // Arrange - task with no candidates (dangling)

        // Act
        userTask.setDangling(false);

        // Assert - dangling should still be true since it's derived
        assertThat(userTask.isDangling()).isTrue();
    }

    // --- Helper methods ---

    private Person createPerson(String id) {
        Person person = new Person();
        person.setId(id);
        person.setFulltext("Person " + id);
        person.setSort(id);
        return person;
    }

    private Group createGroup(String id) {
        Group group = new Group();
        group.setId(id);
        group.setFulltext("Group " + id);
        group.setSort(id);
        return group;
    }
}
