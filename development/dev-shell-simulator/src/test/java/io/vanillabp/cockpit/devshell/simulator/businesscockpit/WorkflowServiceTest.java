package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock
    private WorkflowRepository workflows;

    @InjectMocks
    private WorkflowService workflowService;

    private Workflow activeWorkflow;
    private Workflow completedWorkflow;

    @BeforeEach
    void setUp() {
        // Aktiver Workflow ohne endedAt
        activeWorkflow = new Workflow();
        activeWorkflow.setId("wf-1");
        activeWorkflow.setBusinessId("BIZ-001");
        activeWorkflow.setCreatedAt(OffsetDateTime.now());

        // Abgeschlossener Workflow mit endedAt
        completedWorkflow = new Workflow();
        completedWorkflow.setId("wf-2");
        completedWorkflow.setBusinessId("BIZ-002");
        completedWorkflow.setCreatedAt(OffsetDateTime.now());
        completedWorkflow.setEndedAt(OffsetDateTime.now());
    }

    // --- createWorkflow ---

    @Test
    void createWorkflow_withValidInput_savesWorkflow() {
        // Workflow anlegen
        workflowService.createWorkflow("wf-1", activeWorkflow);

        // Workflow muss im Repository gespeichert werden
        verify(workflows).save(activeWorkflow);
    }

    @Test
    void createWorkflow_withNullId_throwsIllegalArgumentException() {
        // Null-ID muss zu einer IllegalArgumentException fuehren
        assertThatThrownBy(() -> workflowService.createWorkflow(null, activeWorkflow))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void createWorkflow_withNullWorkflow_throwsIllegalArgumentException() {
        // Null-Workflow muss zu einer IllegalArgumentException fuehren
        assertThatThrownBy(() -> workflowService.createWorkflow("wf-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    // --- getWorkflow ---

    @Test
    void getWorkflow_withExistingId_returnsWorkflow() {
        // Repository gibt den Workflow zurueck
        when(workflows.findById("wf-1")).thenReturn(Optional.of(activeWorkflow));

        // Workflow abrufen und pruefen
        final var result = workflowService.getWorkflow("wf-1");

        assertThat(result).isSameAs(activeWorkflow);
    }

    @Test
    void getWorkflow_withNullId_throwsIllegalArgumentException() {
        // Null-ID muss zu einer IllegalArgumentException fuehren
        assertThatThrownBy(() -> workflowService.getWorkflow(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void getWorkflow_withNonExistingId_throwsIllegalStateException() {
        // Repository findet keinen Workflow
        when(workflows.findById("unknown")).thenReturn(Optional.empty());

        // Erwarte IllegalStateException fuer nicht existierenden Workflow
        assertThatThrownBy(() -> workflowService.getWorkflow("unknown"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");
    }

    // --- getAllWorkflows ---

    @Test
    void getAllWorkflows_returnsAllStoredWorkflows() {
        // Repository gibt alle Workflows zurueck
        when(workflows.findAll()).thenReturn(List.of(activeWorkflow, completedWorkflow));

        // Alle Workflows abfragen
        final var result = workflowService.getAllWorkflows();

        assertThat(result).containsExactly(activeWorkflow, completedWorkflow);
    }

    @Test
    void getAllWorkflows_withEmptyRepository_returnsEmptyList() {
        // Repository gibt leere Liste zurueck
        when(workflows.findAll()).thenReturn(List.of());

        // Leere Ergebnismenge pruefen
        final var result = workflowService.getAllWorkflows();

        assertThat(result).isEmpty();
    }

    // --- getWorkflows (paginiert) ---

    @Test
    void getWorkflows_withPagination_returnsCorrectPage() {
        // Drei Workflows anlegen fuer Paginierungstest
        final var wf3 = new Workflow();
        wf3.setId("wf-3");
        when(workflows.findAll()).thenReturn(List.of(activeWorkflow, completedWorkflow, wf3));

        // Erste Seite mit 2 Eintraegen abfragen
        final var firstPage = workflowService.getWorkflows(0, 2);

        assertThat(firstPage.getPageObjects()).hasSize(2);
        assertThat(firstPage.getNumber()).isEqualTo(0);
        assertThat(firstPage.getSize()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }

    @Test
    void getWorkflows_secondPage_returnsRemainingItems() {
        // Drei Workflows fuer zweite Seite
        final var wf3 = new Workflow();
        wf3.setId("wf-3");
        when(workflows.findAll()).thenReturn(List.of(activeWorkflow, completedWorkflow, wf3));

        // Zweite Seite mit 2 Eintraegen abfragen
        final var secondPage = workflowService.getWorkflows(1, 2);

        assertThat(secondPage.getPageObjects()).hasSize(1);
        assertThat(secondPage.getNumber()).isEqualTo(1);
    }

    @Test
    void getWorkflows_withNullPaginationParams_usesDefaults() {
        // Repository gibt einen Workflow zurueck
        when(workflows.findAll()).thenReturn(List.of(activeWorkflow));

        // Abfrage ohne Paginierungsparameter
        final var result = workflowService.getWorkflows(null, null);

        // Default-Werte: pageNumber=0, pageSize=20
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(20);
    }

    @Test
    void getWorkflows_beyondLastPage_returnsEmptyPageObjects() {
        // Repository gibt 2 Workflows zurueck
        when(workflows.findAll()).thenReturn(List.of(activeWorkflow, completedWorkflow));

        // Seitennummer weit jenseits der letzten Seite
        final var result = workflowService.getWorkflows(100, 20);

        assertThat(result.getPageObjects()).isEmpty();
    }

    @Test
    void getWorkflows_withEmptyResult_returnsEmptyPage() {
        // Repository gibt keine Workflows zurueck
        when(workflows.findAll()).thenReturn(List.of());

        // Leere Ergebnismenge abfragen
        final var result = workflowService.getWorkflows(0, 20);

        assertThat(result.getPageObjects()).isEmpty();
        assertThat(result.getTotalPages()).isEqualTo(0);
    }

    // --- updateWorkflow ---

    @Test
    void updateWorkflow_withExistingWorkflow_appliesConsumerAndSaves() {
        // Repository findet den Workflow
        when(workflows.findById("wf-1")).thenReturn(Optional.of(activeWorkflow));

        // Consumer modifiziert den Workflow
        final var consumerCalled = new AtomicBoolean(false);
        workflowService.updateWorkflow("wf-1", workflow -> {
            workflow.setComment("updated");
            consumerCalled.set(true);
        });

        // Consumer muss aufgerufen worden sein
        assertThat(consumerCalled).isTrue();
        assertThat(activeWorkflow.getComment()).isEqualTo("updated");

        // Geaenderter Workflow muss gespeichert werden
        verify(workflows).save(activeWorkflow);
    }

    @Test
    void updateWorkflow_withNullId_throwsIllegalArgumentException() {
        // Null-ID muss zu einer IllegalArgumentException fuehren
        assertThatThrownBy(() -> workflowService.updateWorkflow(null, wf -> {}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void updateWorkflow_withNonExistingWorkflow_throwsIllegalStateException() {
        // Repository findet keinen Workflow
        when(workflows.findById("unknown")).thenReturn(Optional.empty());

        // Erwarte IllegalStateException
        assertThatThrownBy(() -> workflowService.updateWorkflow("unknown", wf -> {}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");
    }

    // --- completeWorkflow ---

    @Test
    void completeWorkflow_withExistingWorkflow_appliesConsumerAndSaves() {
        // Repository findet den Workflow
        when(workflows.findById("wf-1")).thenReturn(Optional.of(activeWorkflow));

        // Consumer setzt endedAt
        workflowService.completeWorkflow("wf-1", wf -> wf.setEndedAt(OffsetDateTime.now()));

        // Workflow muss beendet und gespeichert worden sein
        assertThat(activeWorkflow.getEndedAt()).isNotNull();
        verify(workflows).save(activeWorkflow);
    }

    @Test
    void completeWorkflow_withNullId_throwsIllegalArgumentException() {
        // Null-ID muss zu einer IllegalArgumentException fuehren
        assertThatThrownBy(() -> workflowService.completeWorkflow(null, wf -> {}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void completeWorkflow_withNonExistingWorkflow_throwsIllegalStateException() {
        // Repository findet keinen Workflow
        when(workflows.findById("unknown")).thenReturn(Optional.empty());

        // Erwarte IllegalStateException
        assertThatThrownBy(() -> workflowService.completeWorkflow("unknown", wf -> {}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");
    }

    // --- cancelWorkflow ---

    @Test
    void cancelWorkflow_withExistingWorkflow_appliesConsumerAndSaves() {
        // Repository findet den Workflow
        when(workflows.findById("wf-1")).thenReturn(Optional.of(activeWorkflow));

        // Consumer setzt endedAt
        workflowService.cancelWorkflow("wf-1", wf -> wf.setEndedAt(OffsetDateTime.now()));

        // Workflow muss beendet und gespeichert worden sein
        assertThat(activeWorkflow.getEndedAt()).isNotNull();
        verify(workflows).save(activeWorkflow);
    }

    /**
     * Stellt sicher, dass cancelWorkflow keine Exception wirft, wenn der
     * Workflow nicht existiert (verwendet ifPresent statt orElseThrow).
     */
    @Test
    void cancelWorkflow_withNonExistingWorkflow_doesNothing() {
        // Repository findet keinen Workflow
        when(workflows.findById("unknown")).thenReturn(Optional.empty());

        // cancelWorkflow soll ohne Fehler durchlaufen
        workflowService.cancelWorkflow("unknown", wf -> wf.setEndedAt(OffsetDateTime.now()));

        // findById wurde aufgerufen, aber save nicht
        verify(workflows).findById("unknown");
    }

    @Test
    void cancelWorkflow_withNullId_throwsIllegalArgumentException() {
        // Null-ID muss zu einer IllegalArgumentException fuehren
        assertThatThrownBy(() -> workflowService.cancelWorkflow(null, wf -> {}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

}
