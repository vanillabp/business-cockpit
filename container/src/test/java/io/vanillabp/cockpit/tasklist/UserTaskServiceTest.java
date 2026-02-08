package io.vanillabp.cockpit.tasklist;

import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.cockpit.users.model.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserTaskService}.
 */
@ExtendWith(MockitoExtension.class)
class UserTaskServiceTest {

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
        // Inject the mocked logger
        ReflectionTestUtils.setField(service, "logger", logger);
    }

    // --- getUserTask tests ---

    @Test
    void getUserTask_withValidId_returnsTask() {
        // Arrange
        UserTask task = createUserTask("task-1");
        when(userTasks.findById("task-1")).thenReturn(Mono.just(task));

        // Act & Assert
        StepVerifier.create(service.getUserTask("task-1"))
                .expectNext(task)
                .verifyComplete();
    }

    @Test
    void getUserTask_withNonExistingId_returnsEmpty() {
        // Arrange
        when(userTasks.findById("non-existent")).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(service.getUserTask("non-existent"))
                .verifyComplete();
    }

    // --- markAsRead tests ---

    @Test
    void markAsRead_withValidTaskAndUser_marksTaskAsRead() {
        // Arrange
        UserTask task = createUserTask("task-1");
        when(userTasks.findById("task-1")).thenReturn(Mono.just(task));
        when(userTasks.save(any(UserTask.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // Act & Assert
        StepVerifier.create(service.markAsRead("task-1", "user-1"))
                .expectNextMatches(t -> t.getReadAt("user-1") != null)
                .verifyComplete();

        verify(userTasks).save(any(UserTask.class));
    }

    @Test
    void markAsRead_withNonExistingTask_returnsEmpty() {
        // Arrange
        when(userTasks.findById("non-existent")).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(service.markAsRead("non-existent", "user-1"))
                .verifyComplete();

        verify(userTasks, never()).save(any(UserTask.class));
    }

    @Test
    void markAsRead_withMultipleTasks_marksAllAsRead() {
        // Arrange
        UserTask task1 = createUserTask("task-1");
        UserTask task2 = createUserTask("task-2");
        List<String> taskIds = List.of("task-1", "task-2");

        when(userTasks.findAllById(taskIds)).thenReturn(Flux.just(task1, task2));
        when(userTasks.saveAll(any(Flux.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act & Assert
        StepVerifier.create(service.markAsRead(taskIds, "user-1"))
                .expectNextCount(2)
                .verifyComplete();
    }

    // --- markAsUnread tests ---

    @Test
    void markAsUnread_withValidTaskAndUser_clearsReadAt() {
        // Arrange
        UserTask task = createUserTaskWithMutableLists("task-1");
        task.setReadBy(new ArrayList<>(List.of(new UserTask.ReadBy("user-1", OffsetDateTime.now()))));
        when(userTasks.findById("task-1")).thenReturn(Mono.just(task));
        when(userTasks.save(any(UserTask.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // Act & Assert
        StepVerifier.create(service.markAsUnread("task-1", "user-1"))
                .expectNextMatches(t -> t.getReadAt("user-1") == null)
                .verifyComplete();
    }

    @Test
    void markAsUnread_withMultipleTasks_clearsAllReadAt() {
        // Arrange
        UserTask task1 = createUserTaskWithMutableLists("task-1");
        task1.setReadBy(new ArrayList<>(List.of(new UserTask.ReadBy("user-1", OffsetDateTime.now()))));
        UserTask task2 = createUserTaskWithMutableLists("task-2");
        task2.setReadBy(new ArrayList<>(List.of(new UserTask.ReadBy("user-1", OffsetDateTime.now()))));
        List<String> taskIds = List.of("task-1", "task-2");

        when(userTasks.findAllById(taskIds)).thenReturn(Flux.just(task1, task2));
        when(userTasks.saveAll(any(Flux.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act & Assert
        StepVerifier.create(service.markAsUnread(taskIds, "user-1"))
                .expectNextCount(2)
                .verifyComplete();
    }

    // --- assignTask tests ---

    @Test
    void assignTask_withValidTaskAndPerson_addsCandidatePerson() {
        // Arrange
        UserTask task = createUserTask("task-1");
        Person person = createPerson("person-1", "John Doe");
        when(userTasks.findById("task-1")).thenReturn(Mono.just(task));
        when(userTasks.save(any(UserTask.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // Act & Assert
        StepVerifier.create(service.assignTask("task-1", person))
                .expectNextMatches(t -> t.getCandidateUsers() != null
                        && t.getCandidateUsers().stream().anyMatch(p -> "person-1".equals(p.getId())))
                .verifyComplete();
    }

    @Test
    void assignTask_withMultipleTasks_addsPersonToAll() {
        // Arrange
        UserTask task1 = createUserTask("task-1");
        UserTask task2 = createUserTask("task-2");
        Person person = createPerson("person-1", "John Doe");
        List<String> taskIds = List.of("task-1", "task-2");

        when(userTasks.findAllById(taskIds)).thenReturn(Flux.just(task1, task2));
        when(userTasks.saveAll(any(Flux.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act & Assert
        StepVerifier.create(service.assignTask(taskIds, person))
                .expectNextCount(2)
                .verifyComplete();
    }

    // --- unassignTask tests ---

    @Test
    void unassignTask_withValidTaskAndPerson_removesCandidatePerson() {
        // Arrange
        UserTask task = createUserTaskWithMutableLists("task-1");
        Person person = createPerson("person-1", "John Doe");
        task.setCandidateUsers(new ArrayList<>(List.of(person)));
        when(userTasks.findById("task-1")).thenReturn(Mono.just(task));
        when(userTasks.save(any(UserTask.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // Act & Assert
        StepVerifier.create(service.unassignTask("task-1", "person-1"))
                .expectNextMatches(t -> t.getCandidateUsers() == null
                        || t.getCandidateUsers().stream().noneMatch(p -> "person-1".equals(p.getId())))
                .verifyComplete();
    }

    // --- claimTask tests ---

    @Test
    void claimTask_withValidTaskAndPerson_setsAssignee() {
        // Arrange
        UserTask task = createUserTask("task-1");
        Person person = createPerson("person-1", "John Doe");
        when(userTasks.findById("task-1")).thenReturn(Mono.just(task));
        when(userTasks.save(any(UserTask.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // Act & Assert
        StepVerifier.create(service.claimTask("task-1", person))
                .expectNextMatches(t -> t.getAssignee() != null
                        && "person-1".equals(t.getAssignee().getId()))
                .verifyComplete();

        verify(userTasks).save(any(UserTask.class));
    }

    @Test
    void claimTask_withAlreadyAssignedToSamePerson_doesNotSave() {
        // Arrange
        UserTask task = createUserTask("task-1");
        Person person = createPerson("person-1", "John Doe");
        task.setAssignee(person);
        when(userTasks.findById("task-1")).thenReturn(Mono.just(task));

        // Act & Assert
        StepVerifier.create(service.claimTask("task-1", person))
                .expectNextMatches(t -> t.getAssignee() != null
                        && "person-1".equals(t.getAssignee().getId()))
                .verifyComplete();

        // Should not save when already assigned to the same person
        verify(userTasks, never()).save(any(UserTask.class));
    }

    @Test
    void claimTask_withDifferentAssignee_updatesAssignee() {
        // Arrange
        UserTask task = createUserTask("task-1");
        Person currentAssignee = createPerson("person-1", "John Doe");
        Person newAssignee = createPerson("person-2", "Jane Doe");
        task.setAssignee(currentAssignee);
        when(userTasks.findById("task-1")).thenReturn(Mono.just(task));
        when(userTasks.save(any(UserTask.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // Act & Assert
        StepVerifier.create(service.claimTask("task-1", newAssignee))
                .expectNextMatches(t -> t.getAssignee() != null
                        && "person-2".equals(t.getAssignee().getId()))
                .verifyComplete();

        verify(userTasks).save(any(UserTask.class));
    }

    // --- completeUserTask tests ---

    @Test
    void completeUserTask_withValidTask_setsEndedAtAndReturnsTrue() {
        // Arrange
        UserTask task = createUserTask("task-1");
        OffsetDateTime timestamp = OffsetDateTime.now();
        when(userTasks.save(any(UserTask.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // Act & Assert
        StepVerifier.create(service.completeUserTask(task, timestamp))
                .expectNext(Boolean.TRUE)
                .verifyComplete();

        verify(userTasks).save(any(UserTask.class));
    }

    @Test
    void completeUserTask_withNullTask_returnsFalse() {
        // Act & Assert
        StepVerifier.create(service.completeUserTask(null, OffsetDateTime.now()))
                .expectNext(Boolean.FALSE)
                .verifyComplete();

        verify(userTasks, never()).save(any(UserTask.class));
    }

    @Test
    void completeUserTask_withSaveError_returnsFalse() {
        // Arrange
        UserTask task = createUserTask("task-1");
        OffsetDateTime timestamp = OffsetDateTime.now();
        when(userTasks.save(any(UserTask.class))).thenReturn(Mono.error(new RuntimeException("DB error")));

        // Act & Assert
        StepVerifier.create(service.completeUserTask(task, timestamp))
                .expectNext(Boolean.FALSE)
                .verifyComplete();
    }

    // --- cancelUserTask tests ---

    @Test
    void cancelUserTask_withValidTask_setsEndedAtAndCommentAndReturnsTrue() {
        // Arrange
        UserTask task = createUserTask("task-1");
        OffsetDateTime timestamp = OffsetDateTime.now();
        String reason = "Cancelled by admin";
        when(userTasks.save(any(UserTask.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // Act & Assert
        StepVerifier.create(service.cancelUserTask(task, timestamp, reason))
                .expectNext(Boolean.TRUE)
                .verifyComplete();

        verify(userTasks).save(any(UserTask.class));
    }

    @Test
    void cancelUserTask_withNullTask_returnsFalse() {
        // Act & Assert
        StepVerifier.create(service.cancelUserTask(null, OffsetDateTime.now(), "reason"))
                .expectNext(Boolean.FALSE)
                .verifyComplete();

        verify(userTasks, never()).save(any(UserTask.class));
    }

    // --- createUserTask tests ---

    @Test
    void createUserTask_withValidTask_savesAndReturnsTrue() {
        // Arrange
        UserTask task = createUserTask("task-1");
        task.setDueDate(OffsetDateTime.now().plusDays(1));
        when(userTasks.save(any(UserTask.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // Act & Assert
        StepVerifier.create(service.createUserTask(task))
                .expectNext(Boolean.TRUE)
                .verifyComplete();

        verify(userTasks).save(any(UserTask.class));
    }

    @Test
    void createUserTask_withNullTask_returnsFalse() {
        // Act & Assert
        StepVerifier.create(service.createUserTask(null))
                .expectNext(Boolean.FALSE)
                .verifyComplete();

        verify(userTasks, never()).save(any(UserTask.class));
    }

    @Test
    void createUserTask_withNullDueDate_setsMaxDueDate() {
        // Arrange
        UserTask task = createUserTask("task-1");
        task.setDueDate(null);
        when(userTasks.save(any(UserTask.class))).thenAnswer(inv -> {
            UserTask savedTask = inv.getArgument(0);
            // Verify that dueDate was set to MAX
            return Mono.just(savedTask);
        });

        // Act & Assert
        StepVerifier.create(service.createUserTask(task))
                .expectNext(Boolean.TRUE)
                .verifyComplete();

        verify(userTasks).save(argThat(t ->
                t.getDueDate() != null && t.getDueDate().equals(OffsetDateTime.MAX)));
    }

    // --- updateUserTask tests ---

    @Test
    void updateUserTask_withValidTask_savesAndReturnsTrue() {
        // Arrange
        UserTask task = createUserTask("task-1");
        when(userTasks.save(any(UserTask.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // Act & Assert
        StepVerifier.create(service.updateUserTask(task))
                .expectNext(Boolean.TRUE)
                .verifyComplete();

        verify(userTasks).save(any(UserTask.class));
    }

    @Test
    void updateUserTask_withNullTask_returnsFalse() {
        // Act & Assert
        StepVerifier.create(service.updateUserTask(null))
                .expectNext(Boolean.FALSE)
                .verifyComplete();

        verify(userTasks, never()).save(any(UserTask.class));
    }

    // --- kwic tests ---

    @Test
    void kwic_withEmptyQuery_returnsEmpty() {
        // Act & Assert
        StepVerifier.create(service.kwic(
                        true, false, null, null, null, null,
                        OffsetDateTime.now(), null, "title", ""))
                .verifyComplete();
    }

    @Test
    void kwic_withShortQuery_returnsEmpty() {
        // Act & Assert - query must be at least 3 characters
        StepVerifier.create(service.kwic(
                        true, false, null, null, null, null,
                        OffsetDateTime.now(), null, "title", "ab"))
                .verifyComplete();
    }

    // --- Helper methods ---

    private UserTask createUserTask(String id) {
        UserTask task = new UserTask();
        task.setId(id);
        task.setCreatedAt(OffsetDateTime.now());
        return task;
    }

    private UserTask createUserTaskWithMutableLists(String id) {
        UserTask task = new UserTask();
        task.setId(id);
        task.setCreatedAt(OffsetDateTime.now());
        task.setReadBy(new ArrayList<>());
        task.setCandidateUsers(new ArrayList<>());
        return task;
    }

    private Person createPerson(String id, String name) {
        Person person = new Person();
        person.setId(id);
        person.setFulltext(name);
        return person;
    }
}
