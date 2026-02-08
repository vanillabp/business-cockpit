package io.vanillabp.cockpit.workflowlist.model;

import io.vanillabp.cockpit.tasklist.model.UiUriType;
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
 * Unit tests for {@link Workflow}.
 */
class WorkflowTest {

    private Workflow workflow;

    @BeforeEach
    void setUp() {
        workflow = new Workflow();
    }

    // --- isDangling tests ---

    @Test
    void isDangling_withNoAccessibleUsersOrGroups_returnsTrue() {
        // Arrange - no accessible users or groups set

        // Act & Assert
        assertThat(workflow.isDangling()).isTrue();
    }

    @Test
    void isDangling_withAccessibleUsers_returnsFalse() {
        // Arrange
        Person person = createPerson("user1");
        workflow.setAccessibleToUsers(new ArrayList<>(List.of(person)));

        // Act & Assert
        assertThat(workflow.isDangling()).isFalse();
    }

    @Test
    void isDangling_withAccessibleGroups_returnsFalse() {
        // Arrange
        Group group = createGroup("group1");
        workflow.setAccessibleToGroups(new ArrayList<>(List.of(group)));

        // Act & Assert
        assertThat(workflow.isDangling()).isFalse();
    }

    @Test
    void isDangling_withEmptyAccessibleLists_returnsTrue() {
        // Arrange
        workflow.setAccessibleToUsers(new ArrayList<>());
        workflow.setAccessibleToGroups(new ArrayList<>());

        // Act & Assert
        assertThat(workflow.isDangling()).isTrue();
    }

    @Test
    void isDangling_withBothUsersAndGroups_returnsFalse() {
        // Arrange
        Person person = createPerson("user1");
        Group group = createGroup("group1");
        workflow.setAccessibleToUsers(new ArrayList<>(List.of(person)));
        workflow.setAccessibleToGroups(new ArrayList<>(List.of(group)));

        // Act & Assert
        assertThat(workflow.isDangling()).isFalse();
    }

    // --- addPersonForAccess tests ---

    @Test
    void addPersonForAccess_withNullPerson_doesNothing() {
        // Act
        workflow.addPersonForAccess(null);

        // Assert
        assertThat(workflow.getAccessibleToUsers()).isNull();
    }

    @Test
    void addPersonForAccess_withNoAccessibleUsers_createsListWithPerson() {
        // Arrange
        Person person = createPerson("user1");

        // Act
        workflow.addPersonForAccess(person);

        // Assert
        assertThat(workflow.getAccessibleToUsers()).hasSize(1);
        assertThat(workflow.getAccessibleToUsers().get(0).getId()).isEqualTo("user1");
    }

    @Test
    void addPersonForAccess_withExistingUsers_addsPerson() {
        // Arrange
        Person existingPerson = createPerson("user1");
        workflow.setAccessibleToUsers(new ArrayList<>(List.of(existingPerson)));
        Person newPerson = createPerson("user2");

        // Act
        workflow.addPersonForAccess(newPerson);

        // Assert
        assertThat(workflow.getAccessibleToUsers()).hasSize(2);
    }

    @Test
    void addPersonForAccess_withDuplicatePerson_replacesPerson() {
        // Arrange
        Person person1 = createPerson("user1");
        person1.setFulltext("Original");
        workflow.setAccessibleToUsers(new ArrayList<>(List.of(person1)));

        Person updatedPerson = createPerson("user1");
        updatedPerson.setFulltext("Updated");

        // Act
        workflow.addPersonForAccess(updatedPerson);

        // Assert - should still have only one person with updated data
        assertThat(workflow.getAccessibleToUsers()).hasSize(1);
        assertThat(workflow.getAccessibleToUsers().get(0).getFulltext()).isEqualTo("Updated");
    }

    // --- removeUserForAccess tests ---

    @Test
    void removeUserForAccess_withNullPersonId_doesNothing() {
        // Arrange
        Person person = createPerson("user1");
        workflow.setAccessibleToUsers(new ArrayList<>(List.of(person)));

        // Act
        workflow.removeUserForAccess(null);

        // Assert
        assertThat(workflow.getAccessibleToUsers()).hasSize(1);
    }

    @Test
    void removeUserForAccess_withNoAccessibleUsers_doesNothing() {
        // Act
        workflow.removeUserForAccess("user1");

        // Assert
        assertThat(workflow.getAccessibleToUsers()).isNull();
    }

    @Test
    void removeUserForAccess_withEmptyAccessibleUsers_doesNothing() {
        // Arrange
        workflow.setAccessibleToUsers(new ArrayList<>());

        // Act
        workflow.removeUserForAccess("user1");

        // Assert
        assertThat(workflow.getAccessibleToUsers()).isEmpty();
    }

    @Test
    void removeUserForAccess_withExistingPerson_removesPerson() {
        // Arrange
        Person person1 = createPerson("user1");
        Person person2 = createPerson("user2");
        workflow.setAccessibleToUsers(new ArrayList<>(List.of(person1, person2)));

        // Act
        workflow.removeUserForAccess("user1");

        // Assert
        assertThat(workflow.getAccessibleToUsers()).hasSize(1);
        assertThat(workflow.getAccessibleToUsers().get(0).getId()).isEqualTo("user2");
    }

    @Test
    void removeUserForAccess_withNonExistentPerson_doesNothing() {
        // Arrange
        Person person1 = createPerson("user1");
        workflow.setAccessibleToUsers(new ArrayList<>(List.of(person1)));

        // Act
        workflow.removeUserForAccess("user2");

        // Assert
        assertThat(workflow.getAccessibleToUsers()).hasSize(1);
    }

    // --- Basic getter/setter tests ---

    @Test
    void settersAndGetters_workCorrectly() {
        // Arrange
        OffsetDateTime now = OffsetDateTime.now();
        Map<String, String> title = Map.of("en", "Test Workflow");
        Person initiator = createPerson("initiator");

        // Act
        workflow.setId("workflow-123");
        workflow.setVersion(1L);
        workflow.setInitiator(initiator);
        workflow.setCreatedAt(now);
        workflow.setUpdatedAt(now);
        workflow.setUpdatedBy("updater");
        workflow.setEndedAt(now);
        workflow.setSource("test-source");
        workflow.setWorkflowModuleId("module-1");
        workflow.setComment("Test comment");
        workflow.setBpmnProcessId("process-1");
        workflow.setBpmnProcessVersion("1.0");
        workflow.setBusinessId("business-123");
        workflow.setTitle(title);
        workflow.setUiUriPath("/workflows/123");
        workflow.setUiUriType(UiUriType.EXTERNAL);
        workflow.setDetails(Map.of("key", "value"));
        workflow.setDetailsFulltextSearch("fulltext search");

        // Assert
        assertThat(workflow.getId()).isEqualTo("workflow-123");
        assertThat(workflow.getVersion()).isEqualTo(1L);
        assertThat(workflow.getInitiator()).isEqualTo(initiator);
        assertThat(workflow.getCreatedAt()).isEqualTo(now);
        assertThat(workflow.getUpdatedAt()).isEqualTo(now);
        assertThat(workflow.getUpdatedBy()).isEqualTo("updater");
        assertThat(workflow.getEndedAt()).isEqualTo(now);
        assertThat(workflow.getSource()).isEqualTo("test-source");
        assertThat(workflow.getWorkflowModuleId()).isEqualTo("module-1");
        assertThat(workflow.getComment()).isEqualTo("Test comment");
        assertThat(workflow.getBpmnProcessId()).isEqualTo("process-1");
        assertThat(workflow.getBpmnProcessVersion()).isEqualTo("1.0");
        assertThat(workflow.getBusinessId()).isEqualTo("business-123");
        assertThat(workflow.getTitle()).isEqualTo(title);
        assertThat(workflow.getUiUriPath()).isEqualTo("/workflows/123");
        assertThat(workflow.getUiUriType()).isEqualTo(UiUriType.EXTERNAL);
        assertThat(workflow.getDetails()).containsEntry("key", "value");
        assertThat(workflow.getDetailsFulltextSearch()).isEqualTo("fulltext search");
    }

    @Test
    void setDangling_isIgnored() {
        // Arrange - workflow with no accessible users/groups (dangling)

        // Act
        workflow.setDangling(false);

        // Assert - dangling should still be true since it's derived
        assertThat(workflow.isDangling()).isTrue();
    }

    @Test
    void collectionName_isCorrect() {
        // Assert
        assertThat(Workflow.COLLECTION_NAME).isEqualTo("workflow");
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
