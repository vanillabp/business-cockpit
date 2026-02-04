package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.devshell.simulator.businesscockpit.TaskService.RetrieveMode;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Page;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTaskRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private UserTaskRepository userTasks;

    @InjectMocks
    private TaskService taskService;

    private UserTask openTask;
    private UserTask closedTask;

    @BeforeEach
    void setUp() {
        // Offene Aufgabe ohne endedAt
        openTask = new UserTask();
        openTask.setId("task-1");
        openTask.setWorkflowId("wf-1");
        openTask.setCreatedAt(OffsetDateTime.now());

        // Abgeschlossene Aufgabe mit endedAt
        closedTask = new UserTask();
        closedTask.setId("task-2");
        closedTask.setWorkflowId("wf-1");
        closedTask.setCreatedAt(OffsetDateTime.now());
        closedTask.setEndedAt(OffsetDateTime.now());
    }

    // --- createTask ---

    @Test
    void createTask_withValidInput_savesTask() {
        // Aufgabe mit gueltigem Input anlegen
        taskService.createTask("task-1", openTask);

        // Sicherstellen, dass die Aufgabe im Repository gespeichert wird
        verify(userTasks).save(openTask);
    }

    @Test
    void createTask_withNullId_throwsIllegalArgumentException() {
        // Null-ID muss zu einer IllegalArgumentException fuehren
        assertThatThrownBy(() -> taskService.createTask(null, openTask))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void createTask_withNullTask_throwsIllegalArgumentException() {
        // Null-Aufgabe muss zu einer IllegalArgumentException fuehren
        assertThatThrownBy(() -> taskService.createTask("task-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    // --- getUserTask ---

    @Test
    void getUserTask_withExistingId_returnsTask() {
        // Repository gibt die Aufgabe zurueck
        when(userTasks.findById("task-1")).thenReturn(Optional.of(openTask));

        // Aufgabe abrufen und pruefen
        final var result = taskService.getUserTask("task-1");

        assertThat(result).isSameAs(openTask);
    }

    @Test
    void getUserTask_withNullId_throwsIllegalArgumentException() {
        // Null-ID muss zu einer IllegalArgumentException fuehren
        assertThatThrownBy(() -> taskService.getUserTask(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void getUserTask_withNonExistingId_throwsIllegalStateException() {
        // Repository findet keine Aufgabe
        when(userTasks.findById("unknown")).thenReturn(Optional.empty());

        // Erwarte IllegalStateException fuer nicht existierende Aufgabe
        assertThatThrownBy(() -> taskService.getUserTask("unknown"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");
    }

    // --- getUserTasks mit RetrieveMode ---

    @Test
    void getUserTasks_withAllMode_returnsAllTasks() {
        // Repository gibt alle Aufgaben zurueck
        when(userTasks.findAll()).thenReturn(List.of(openTask, closedTask));

        // Alle Aufgaben im ALL-Modus abfragen
        final var result = taskService.getUserTasks(RetrieveMode.ALL, 0, 20);

        assertThat(result.getPageObjects()).containsExactly(openTask, closedTask);
    }

    @Test
    void getUserTasks_withOpenTasksMode_returnsOnlyOpenTasks() {
        // Repository gibt nur offene Aufgaben zurueck
        when(userTasks.findByEndedAtIsNull()).thenReturn(List.of(openTask));

        // Nur offene Aufgaben im OPENTASKS-Modus abfragen
        final var result = taskService.getUserTasks(RetrieveMode.OPENTASKS, 0, 20);

        assertThat(result.getPageObjects()).containsExactly(openTask);
    }

    @Test
    void getUserTasks_withClosedTasksMode_returnsOnlyClosedTasks() {
        // Repository gibt nur abgeschlossene Aufgaben zurueck
        when(userTasks.findByEndedAtIsNotNull()).thenReturn(List.of(closedTask));

        // Nur abgeschlossene Aufgaben im CLOSEDTASKSONLY-Modus abfragen
        final var result = taskService.getUserTasks(RetrieveMode.CLOSEDTASKSONLY, 0, 20);

        assertThat(result.getPageObjects()).containsExactly(closedTask);
    }

    /**
     * Verifiziert, dass die Retrieve-Modi OPENTASKSWITHOUTFOLLOWUP und
     * OPENTASKSWITHFOLLOWUP auf den Default-Zweig (findAll) fallen.
     */
    @Test
    void getUserTasks_withFallbackModes_returnsAllTasks() {
        // Repository gibt alle Aufgaben zurueck (Default-Zweig)
        when(userTasks.findAll()).thenReturn(List.of(openTask, closedTask));

        // OPENTASKSWITHOUTFOLLOWUP faellt auf den Default-Zweig
        final var resultWithout = taskService.getUserTasks(RetrieveMode.OPENTASKSWITHOUTFOLLOWUP, 0, 20);
        assertThat(resultWithout.getPageObjects()).hasSize(2);

        // OPENTASKSWITHFOLLOWUP faellt ebenfalls auf den Default-Zweig
        final var resultWith = taskService.getUserTasks(RetrieveMode.OPENTASKSWITHFOLLOWUP, 0, 20);
        assertThat(resultWith.getPageObjects()).hasSize(2);
    }

    // --- Paginierung ---

    @Test
    void getUserTasks_withPagination_returnsCorrectPage() {
        // Drei Aufgaben anlegen, um Paginierung mit Seitengroesse 2 zu testen
        final var task3 = new UserTask();
        task3.setId("task-3");
        when(userTasks.findAll()).thenReturn(List.of(openTask, closedTask, task3));

        // Erste Seite mit 2 Eintraegen abfragen
        final var firstPage = taskService.getUserTasks(RetrieveMode.ALL, 0, 2);
        assertThat(firstPage.getPageObjects()).hasSize(2);
        assertThat(firstPage.getNumber()).isEqualTo(0);
        assertThat(firstPage.getSize()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);

        // Zweite Seite mit 1 Eintrag abfragen
        final var secondPage = taskService.getUserTasks(RetrieveMode.ALL, 1, 2);
        assertThat(secondPage.getPageObjects()).hasSize(1);
        assertThat(secondPage.getNumber()).isEqualTo(1);
    }

    @Test
    void getUserTasks_withNullPaginationParams_usesDefaults() {
        // Repository gibt eine Aufgabe zurueck
        when(userTasks.findAll()).thenReturn(List.of(openTask));

        // Abfrage ohne Paginierungsparameter (null-Werte verwenden Defaults)
        final var result = taskService.getUserTasks(RetrieveMode.ALL, null, null);

        // Default-Werte: pageNumber=0, pageSize=20
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(20);
    }

    @Test
    void getUserTasks_beyondLastPage_returnsEmptyPageObjects() {
        // Repository gibt 2 Aufgaben zurueck
        when(userTasks.findAll()).thenReturn(List.of(openTask, closedTask));

        // Seitennummer liegt jenseits der letzten Seite
        final var result = taskService.getUserTasks(RetrieveMode.ALL, 100, 20);

        assertThat(result.getPageObjects()).isEmpty();
    }

    @Test
    void getUserTasks_withEmptyResult_returnsEmptyPage() {
        // Repository gibt keine Aufgaben zurueck
        when(userTasks.findAll()).thenReturn(List.of());

        // Leere Ergebnismenge abfragen
        final var result = taskService.getUserTasks(RetrieveMode.ALL, 0, 20);

        assertThat(result.getPageObjects()).isEmpty();
        assertThat(result.getTotalPages()).isEqualTo(0);
    }

    // --- getUserTasksOfWorkflow ---

    @Test
    void getUserTasksOfWorkflow_withOpenTasksMode_filtersCorrectly() {
        // Repository gibt offene Aufgaben eines bestimmten Workflows zurueck
        when(userTasks.findByWorkflowIdAndEndedAtIsNull("wf-1")).thenReturn(List.of(openTask));

        // Offene Aufgaben eines Workflows abfragen
        final var result = taskService.getUserTasksOfWorkflow("wf-1", RetrieveMode.OPENTASKS, 0, 20);

        assertThat(result.getPageObjects()).containsExactly(openTask);
    }

    @Test
    void getUserTasksOfWorkflow_withClosedTasksMode_filtersCorrectly() {
        // Repository gibt abgeschlossene Aufgaben eines bestimmten Workflows zurueck
        when(userTasks.findByWorkflowIdAndEndedAtIsNotNull("wf-1")).thenReturn(List.of(closedTask));

        // Abgeschlossene Aufgaben eines Workflows abfragen
        final var result = taskService.getUserTasksOfWorkflow("wf-1", RetrieveMode.CLOSEDTASKSONLY, 0, 20);

        assertThat(result.getPageObjects()).containsExactly(closedTask);
    }

    @Test
    void getUserTasksOfWorkflow_withAllMode_returnsAllTasksOfWorkflow() {
        // Repository gibt alle Aufgaben des Workflows zurueck
        when(userTasks.findByWorkflowId("wf-1")).thenReturn(List.of(openTask, closedTask));

        // Alle Aufgaben des Workflows abfragen
        final var result = taskService.getUserTasksOfWorkflow("wf-1", RetrieveMode.ALL, 0, 20);

        assertThat(result.getPageObjects()).containsExactly(openTask, closedTask);
    }

    // --- getAllUserTasks ---

    @Test
    void getAllUserTasks_returnsAllStoredTasks() {
        // Repository gibt alle gespeicherten Aufgaben zurueck
        when(userTasks.findAll()).thenReturn(List.of(openTask, closedTask));

        // Alle Aufgaben abfragen
        final var result = taskService.getAllUserTasks();

        assertThat(result).containsExactly(openTask, closedTask);
    }

    // --- updateTask ---

    @Test
    void updateTask_withExistingTask_appliesConsumerAndSaves() {
        // Repository findet die Aufgabe
        when(userTasks.findById("task-1")).thenReturn(Optional.of(openTask));

        // Consumer, der die Aufgabe modifiziert
        final var consumerCalled = new AtomicBoolean(false);
        taskService.updateTask("task-1", task -> {
            task.setComment("updated");
            consumerCalled.set(true);
        });

        // Consumer muss aufgerufen worden sein
        assertThat(consumerCalled).isTrue();
        assertThat(openTask.getComment()).isEqualTo("updated");

        // Geaenderte Aufgabe muss gespeichert werden
        verify(userTasks).save(openTask);
    }

    @Test
    void updateTask_withNullId_throwsIllegalArgumentException() {
        // Null-ID muss zu einer IllegalArgumentException fuehren
        assertThatThrownBy(() -> taskService.updateTask(null, task -> {}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    @Test
    void updateTask_withNonExistingTask_throwsIllegalStateException() {
        // Repository findet keine Aufgabe
        when(userTasks.findById("unknown")).thenReturn(Optional.empty());

        // Erwarte IllegalStateException
        assertThatThrownBy(() -> taskService.updateTask("unknown", task -> {}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");
    }

    // --- completeTask ---

    @Test
    void completeTask_withExistingTask_appliesConsumerAndSaves() {
        // Repository findet die Aufgabe
        when(userTasks.findById("task-1")).thenReturn(Optional.of(openTask));

        // Consumer setzt endedAt
        taskService.completeTask("task-1", task -> task.setEndedAt(OffsetDateTime.now()));

        // Aufgabe muss beendet und gespeichert worden sein
        assertThat(openTask.getEndedAt()).isNotNull();
        verify(userTasks).save(openTask);
    }

    @Test
    void completeTask_withNullId_throwsIllegalArgumentException() {
        // Null-ID muss zu einer IllegalArgumentException fuehren
        assertThatThrownBy(() -> taskService.completeTask(null, task -> {}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    @Test
    void completeTask_withNonExistingTask_throwsIllegalStateException() {
        // Repository findet keine Aufgabe
        when(userTasks.findById("unknown")).thenReturn(Optional.empty());

        // Erwarte IllegalStateException
        assertThatThrownBy(() -> taskService.completeTask("unknown", task -> {}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");
    }

    // --- cancelTask ---

    @Test
    void cancelTask_withExistingTask_appliesConsumerAndSaves() {
        // Repository findet die Aufgabe
        when(userTasks.findById("task-1")).thenReturn(Optional.of(openTask));

        // Consumer setzt endedAt
        taskService.cancelTask("task-1", task -> task.setEndedAt(OffsetDateTime.now()));

        // Aufgabe muss beendet und gespeichert worden sein
        assertThat(openTask.getEndedAt()).isNotNull();
        verify(userTasks).save(openTask);
    }

    /**
     * Stellt sicher, dass cancelTask keine Exception wirft, wenn die
     * Aufgabe nicht existiert (im Gegensatz zu completeTask).
     */
    @Test
    void cancelTask_withNonExistingTask_doesNothing() {
        // Repository findet keine Aufgabe
        when(userTasks.findById("unknown")).thenReturn(Optional.empty());

        // cancelTask soll ohne Fehler durchlaufen
        taskService.cancelTask("unknown", task -> task.setEndedAt(OffsetDateTime.now()));

        // save darf nicht aufgerufen worden sein
        verify(userTasks).findById("unknown");
    }

    @Test
    void cancelTask_withNullId_throwsIllegalArgumentException() {
        // Null-ID muss zu einer IllegalArgumentException fuehren
        assertThatThrownBy(() -> taskService.cancelTask(null, task -> {}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

}
