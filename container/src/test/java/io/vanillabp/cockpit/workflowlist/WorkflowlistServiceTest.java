package io.vanillabp.cockpit.workflowlist;

import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.cockpit.workflowlist.model.WorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WorkflowlistService}.
 */
@ExtendWith(MockitoExtension.class)
class WorkflowlistServiceTest {

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private ReactiveMongoTemplate mongoTemplate;

    @Mock
    private Logger logger;

    @InjectMocks
    private WorkflowlistService service;

    @BeforeEach
    void setUp() {
        // Inject the mocked logger
        ReflectionTestUtils.setField(service, "logger", logger);
    }

    // --- getWorkflow tests ---

    @Test
    void getWorkflow_withValidId_returnsWorkflow() {
        // Arrange
        Workflow workflow = createWorkflow("workflow-1");
        when(workflowRepository.findById("workflow-1")).thenReturn(Mono.just(workflow));

        // Act & Assert
        StepVerifier.create(service.getWorkflow("workflow-1"))
                .expectNext(workflow)
                .verifyComplete();
    }

    @Test
    void getWorkflow_withNonExistingId_returnsEmpty() {
        // Arrange
        when(workflowRepository.findById("non-existent")).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(service.getWorkflow("non-existent"))
                .verifyComplete();
    }

    // --- createWorkflow tests ---

    @Test
    void createWorkflow_withValidWorkflow_savesAndReturnsTrue() {
        // Arrange
        Workflow workflow = createWorkflow("workflow-1");
        when(workflowRepository.save(any(Workflow.class))).thenReturn(Mono.just(workflow));

        // Act & Assert
        StepVerifier.create(service.createWorkflow(workflow))
                .expectNext(Boolean.TRUE)
                .verifyComplete();

        verify(workflowRepository).save(any(Workflow.class));
    }

    @Test
    void createWorkflow_withNullWorkflow_returnsFalse() {
        // Act & Assert
        StepVerifier.create(service.createWorkflow(null))
                .expectNext(Boolean.FALSE)
                .verifyComplete();

        verify(workflowRepository, never()).save(any(Workflow.class));
    }

    @Test
    void createWorkflow_withSaveError_returnsFalse() {
        // Arrange
        Workflow workflow = createWorkflow("workflow-1");
        when(workflowRepository.save(any(Workflow.class)))
                .thenReturn(Mono.error(new RuntimeException("DB error")));

        // Act & Assert
        StepVerifier.create(service.createWorkflow(workflow))
                .expectNext(Boolean.FALSE)
                .verifyComplete();
    }

    // --- updateWorkflow tests ---

    @Test
    void updateWorkflow_withValidWorkflow_savesAndReturnsTrue() {
        // Arrange
        Workflow workflow = createWorkflow("workflow-1");
        when(workflowRepository.save(any(Workflow.class))).thenReturn(Mono.just(workflow));

        // Act & Assert
        StepVerifier.create(service.updateWorkflow(workflow))
                .expectNext(Boolean.TRUE)
                .verifyComplete();

        verify(workflowRepository).save(any(Workflow.class));
    }

    @Test
    void updateWorkflow_withNullWorkflow_returnsFalse() {
        // Act & Assert
        StepVerifier.create(service.updateWorkflow(null))
                .expectNext(Boolean.FALSE)
                .verifyComplete();

        verify(workflowRepository, never()).save(any(Workflow.class));
    }

    // --- completeWorkflow tests ---

    @Test
    void completeWorkflow_withValidWorkflow_setsEndedAtAndReturnsTrue() {
        // Arrange
        Workflow workflow = createWorkflow("workflow-1");
        OffsetDateTime timestamp = OffsetDateTime.now();
        when(workflowRepository.save(any(Workflow.class))).thenReturn(Mono.just(workflow));

        // Act & Assert
        StepVerifier.create(service.completeWorkflow(workflow, timestamp))
                .expectNext(Boolean.TRUE)
                .verifyComplete();

        verify(workflowRepository).save(any(Workflow.class));
    }

    @Test
    void completeWorkflow_withNullWorkflow_returnsFalse() {
        // Act & Assert
        StepVerifier.create(service.completeWorkflow(null, OffsetDateTime.now()))
                .expectNext(Boolean.FALSE)
                .verifyComplete();

        verify(workflowRepository, never()).save(any(Workflow.class));
    }

    @Test
    void completeWorkflow_withSaveError_returnsFalse() {
        // Arrange
        Workflow workflow = createWorkflow("workflow-1");
        OffsetDateTime timestamp = OffsetDateTime.now();
        when(workflowRepository.save(any(Workflow.class)))
                .thenReturn(Mono.error(new RuntimeException("DB error")));

        // Act & Assert
        StepVerifier.create(service.completeWorkflow(workflow, timestamp))
                .expectNext(Boolean.FALSE)
                .verifyComplete();
    }

    // --- cancelWorkflow tests ---

    @Test
    void cancelWorkflow_withValidWorkflow_setsEndedAtAndCommentAndReturnsTrue() {
        // Arrange
        Workflow workflow = createWorkflow("workflow-1");
        OffsetDateTime timestamp = OffsetDateTime.now();
        String reason = "Cancelled by admin";
        when(workflowRepository.save(any(Workflow.class))).thenReturn(Mono.just(workflow));

        // Act & Assert
        StepVerifier.create(service.cancelWorkflow(workflow, timestamp, reason))
                .expectNext(Boolean.TRUE)
                .verifyComplete();

        verify(workflowRepository).save(any(Workflow.class));
    }

    @Test
    void cancelWorkflow_withNullWorkflow_returnsFalse() {
        // Act & Assert
        StepVerifier.create(service.cancelWorkflow(null, OffsetDateTime.now(), "reason"))
                .expectNext(Boolean.FALSE)
                .verifyComplete();

        verify(workflowRepository, never()).save(any(Workflow.class));
    }

    @Test
    void cancelWorkflow_withSaveError_returnsFalse() {
        // Arrange
        Workflow workflow = createWorkflow("workflow-1");
        OffsetDateTime timestamp = OffsetDateTime.now();
        String reason = "Cancelled by admin";
        when(workflowRepository.save(any(Workflow.class)))
                .thenReturn(Mono.error(new RuntimeException("DB error")));

        // Act & Assert
        StepVerifier.create(service.cancelWorkflow(workflow, timestamp, reason))
                .expectNext(Boolean.FALSE)
                .verifyComplete();
    }

    // --- kwic tests ---

    @Test
    void kwic_withEmptyQuery_returnsEmpty() {
        // Act & Assert
        StepVerifier.create(service.kwic(
                        OffsetDateTime.now(), true, null, null, null, "title", ""))
                .verifyComplete();
    }

    @Test
    void kwic_withShortQuery_returnsEmpty() {
        // Act & Assert - query must be at least 3 characters
        StepVerifier.create(service.kwic(
                        OffsetDateTime.now(), true, null, null, null, "title", "ab"))
                .verifyComplete();
    }

    @Test
    void kwic_withNullQuery_returnsEmpty() {
        // Act & Assert
        StepVerifier.create(service.kwic(
                        OffsetDateTime.now(), true, null, null, null, "title", null))
                .verifyComplete();
    }

    // --- Helper methods ---

    private Workflow createWorkflow(String id) {
        Workflow workflow = new Workflow();
        workflow.setId(id);
        workflow.setCreatedAt(OffsetDateTime.now());
        return workflow;
    }
}
