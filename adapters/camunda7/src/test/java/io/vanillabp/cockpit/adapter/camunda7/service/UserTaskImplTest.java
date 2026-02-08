package io.vanillabp.cockpit.adapter.camunda7.service;

import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskEventImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserTaskImpl}.
 */
@ExtendWith(MockitoExtension.class)
class UserTaskImplTest {

    @Mock
    private UserTaskEventImpl event;

    private UserTaskImpl userTask;

    @BeforeEach
    void setUp() {
        userTask = new UserTaskImpl(event);
    }

    @Test
    void getId_delegatesToEvent() {
        // Arrange
        when(event.getUserTaskId()).thenReturn("task-123");

        // Act
        String result = userTask.getId();

        // Assert
        assertThat(result).isEqualTo("task-123");
    }

    @Test
    void getInitiator_delegatesToEvent() {
        // Arrange
        when(event.getInitiator()).thenReturn("user@example.com");

        // Act
        String result = userTask.getInitiator();

        // Assert
        assertThat(result).isEqualTo("user@example.com");
    }

    @Test
    void getComment_delegatesToEvent() {
        // Arrange
        when(event.getComment()).thenReturn("This is a comment");

        // Act
        String result = userTask.getComment();

        // Assert
        assertThat(result).isEqualTo("This is a comment");
    }

    @Test
    void getBpmnProcessId_delegatesToEvent() {
        // Arrange
        when(event.getBpmnProcessId()).thenReturn("test-process");

        // Act
        String result = userTask.getBpmnProcessId();

        // Assert
        assertThat(result).isEqualTo("test-process");
    }

    @Test
    void getBpmnProcessVersion_delegatesToEvent() {
        // Arrange
        when(event.getBpmnProcessVersion()).thenReturn("2.0");

        // Act
        String result = userTask.getBpmnProcessVersion();

        // Assert
        assertThat(result).isEqualTo("2.0");
    }

    @Test
    void getWorkflowTitle_delegatesToEvent() {
        // Arrange
        Map<String, String> titles = Map.of("en", "Workflow Title", "de", "Workflow Titel");
        when(event.getWorkflowTitle()).thenReturn(titles);

        // Act
        Map<String, String> result = userTask.getWorkflowTitle();

        // Assert
        assertThat(result).isEqualTo(titles);
    }

    @Test
    void getTitle_delegatesToEvent() {
        // Arrange
        Map<String, String> titles = Map.of("en", "Task Title", "de", "Aufgaben Titel");
        when(event.getTitle()).thenReturn(titles);

        // Act
        Map<String, String> result = userTask.getTitle();

        // Assert
        assertThat(result).isEqualTo(titles);
    }

    @Test
    void getBpmnTaskId_delegatesToEvent() {
        // Arrange
        when(event.getBpmnTaskId()).thenReturn("UserTask_1");

        // Act
        String result = userTask.getBpmnTaskId();

        // Assert
        assertThat(result).isEqualTo("UserTask_1");
    }

    @Test
    void getTaskDefinition_delegatesToEvent() {
        // Arrange
        when(event.getTaskDefinition()).thenReturn("reviewTask");

        // Act
        String result = userTask.getTaskDefinition();

        // Assert
        assertThat(result).isEqualTo("reviewTask");
    }

    @Test
    void getTaskDefinitionTitle_delegatesToEvent() {
        // Arrange
        Map<String, String> titles = Map.of("en", "Review", "de", "Überprüfung");
        when(event.getTaskDefinitionTitle()).thenReturn(titles);

        // Act
        Map<String, String> result = userTask.getTaskDefinitionTitle();

        // Assert
        assertThat(result).isEqualTo(titles);
    }

    @Test
    void getAssignee_delegatesToEvent() {
        // Arrange
        when(event.getAssignee()).thenReturn("john.doe");

        // Act
        String result = userTask.getAssignee();

        // Assert
        assertThat(result).isEqualTo("john.doe");
    }

    @Test
    void getCandidateUsers_delegatesToEvent() {
        // Arrange
        List<String> candidates = List.of("user1", "user2", "user3");
        when(event.getCandidateUsers()).thenReturn(candidates);

        // Act
        List<String> result = userTask.getCandidateUsers();

        // Assert
        assertThat(result).isEqualTo(candidates);
    }

    @Test
    void getCandidateGroups_delegatesToEvent() {
        // Arrange
        List<String> groups = List.of("reviewers", "managers");
        when(event.getCandidateGroups()).thenReturn(groups);

        // Act
        List<String> result = userTask.getCandidateGroups();

        // Assert
        assertThat(result).isEqualTo(groups);
    }

    @Test
    void getDueDate_delegatesToEvent() {
        // Arrange
        OffsetDateTime dueDate = OffsetDateTime.now().plusDays(7);
        when(event.getDueDate()).thenReturn(dueDate);

        // Act
        OffsetDateTime result = userTask.getDueDate();

        // Assert
        assertThat(result).isEqualTo(dueDate);
    }

    @Test
    void getFollowUpDate_delegatesToEvent() {
        // Arrange
        OffsetDateTime followUpDate = OffsetDateTime.now().plusDays(3);
        when(event.getFollowUpDate()).thenReturn(followUpDate);

        // Act
        OffsetDateTime result = userTask.getFollowUpDate();

        // Assert
        assertThat(result).isEqualTo(followUpDate);
    }

    @Test
    void getDetails_delegatesToEvent() {
        // Arrange
        Map<String, Object> details = Map.of("orderId", 12345, "amount", 100.50);
        when(event.getDetails()).thenReturn(details);

        // Act
        Map<String, Object> result = userTask.getDetails();

        // Assert
        assertThat(result).isEqualTo(details);
    }

    @Test
    void getI18nLanguages_delegatesToEvent() {
        // Arrange
        List<String> languages = List.of("en", "de", "fr");
        when(event.getI18nLanguages()).thenReturn(languages);

        // Act
        List<String> result = userTask.getI18nLanguages();

        // Assert
        assertThat(result).isEqualTo(languages);
    }
}
