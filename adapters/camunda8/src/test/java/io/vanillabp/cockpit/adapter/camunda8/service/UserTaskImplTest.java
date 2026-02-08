package io.vanillabp.cockpit.adapter.camunda8.service;

import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskEventImpl;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserTaskImplTest {

    @Mock
    private UserTaskEventImpl eventImpl;

    private UserTaskImpl userTask;

    @BeforeEach
    void setUp() {
        userTask = new UserTaskImpl(eventImpl);
    }

    @Test
    void getId_delegatesToEvent() {
        when(eventImpl.getUserTaskId()).thenReturn("task-123");
        assertThat(userTask.getId()).isEqualTo("task-123");
    }

    @Test
    void getInitiator_delegatesToEvent() {
        when(eventImpl.getInitiator()).thenReturn("john.doe");
        assertThat(userTask.getInitiator()).isEqualTo("john.doe");
    }

    @Test
    void getComment_delegatesToEvent() {
        when(eventImpl.getComment()).thenReturn("This is a comment");
        assertThat(userTask.getComment()).isEqualTo("This is a comment");
    }

    @Test
    void getBpmnProcessId_delegatesToEvent() {
        when(eventImpl.getBpmnProcessId()).thenReturn("order-process");
        assertThat(userTask.getBpmnProcessId()).isEqualTo("order-process");
    }

    @Test
    void getBpmnProcessVersion_delegatesToEvent() {
        when(eventImpl.getBpmnProcessVersion()).thenReturn("v1.0");
        assertThat(userTask.getBpmnProcessVersion()).isEqualTo("v1.0");
    }

    @Test
    void getWorkflowTitle_delegatesToEvent() {
        final var title = Map.of("en", "Order Process", "de", "Bestellprozess");
        when(eventImpl.getWorkflowTitle()).thenReturn(title);
        assertThat(userTask.getWorkflowTitle()).isEqualTo(title);
    }

    @Test
    void getTitle_delegatesToEvent() {
        final var title = Map.of("en", "Review Order", "de", "Bestellung prüfen");
        when(eventImpl.getTitle()).thenReturn(title);
        assertThat(userTask.getTitle()).isEqualTo(title);
    }

    @Test
    void getBpmnTaskId_delegatesToEvent() {
        when(eventImpl.getBpmnTaskId()).thenReturn("Activity_ReviewOrder");
        assertThat(userTask.getBpmnTaskId()).isEqualTo("Activity_ReviewOrder");
    }

    @Test
    void getTaskDefinition_delegatesToEvent() {
        when(eventImpl.getTaskDefinition()).thenReturn("review-task");
        assertThat(userTask.getTaskDefinition()).isEqualTo("review-task");
    }

    @Test
    void getTaskDefinitionTitle_delegatesToEvent() {
        final var title = Map.of("en", "Review Task", "de", "Prüfaufgabe");
        when(eventImpl.getTaskDefinitionTitle()).thenReturn(title);
        assertThat(userTask.getTaskDefinitionTitle()).isEqualTo(title);
    }

    @Test
    void getAssignee_delegatesToEvent() {
        when(eventImpl.getAssignee()).thenReturn("john.doe");
        assertThat(userTask.getAssignee()).isEqualTo("john.doe");
    }

    @Test
    void getCandidateUsers_delegatesToEvent() {
        final var users = List.of("user1", "user2");
        when(eventImpl.getCandidateUsers()).thenReturn(users);
        assertThat(userTask.getCandidateUsers()).isEqualTo(users);
    }

    @Test
    void getCandidateGroups_delegatesToEvent() {
        final var groups = List.of("managers", "supervisors");
        when(eventImpl.getCandidateGroups()).thenReturn(groups);
        assertThat(userTask.getCandidateGroups()).isEqualTo(groups);
    }

    @Test
    void getDueDate_delegatesToEvent() {
        final var dueDate = OffsetDateTime.now().plusDays(7);
        when(eventImpl.getDueDate()).thenReturn(dueDate);
        assertThat(userTask.getDueDate()).isEqualTo(dueDate);
    }

    @Test
    void getFollowUpDate_delegatesToEvent() {
        final var followUpDate = OffsetDateTime.now().plusDays(3);
        when(eventImpl.getFollowUpDate()).thenReturn(followUpDate);
        assertThat(userTask.getFollowUpDate()).isEqualTo(followUpDate);
    }

    @Test
    void getDetails_delegatesToEvent() {
        final var details = Map.of("orderId", (Object) "ORD-123", "amount", 99.99);
        when(eventImpl.getDetails()).thenReturn(details);
        assertThat(userTask.getDetails()).isEqualTo(details);
    }

    @Test
    void getI18nLanguages_delegatesToEvent() {
        final var languages = List.of("en", "de", "fr");
        when(eventImpl.getI18nLanguages()).thenReturn(languages);
        assertThat(userTask.getI18nLanguages()).isEqualTo(languages);
    }

}
