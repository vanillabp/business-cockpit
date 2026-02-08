package io.vanillabp.cockpit.adapter.camunda8.deployments.jpa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for JpaProcessInstancePersistence.
 */
@ExtendWith(MockitoExtension.class)
class JpaProcessInstancePersistenceTest {

    @Mock
    private ProcessInstanceRepository processInstanceRepository;

    private JpaProcessInstancePersistence persistence;

    @BeforeEach
    void setUp() {
        persistence = new JpaProcessInstancePersistence(processInstanceRepository);
    }

    @Test
    void save_createsAndSavesProcessInstance() {
        // Prepare test data
        final long processInstanceKey = 12345L;
        final String businessKey = "ORDER-123";
        final String bpmnProcessId = "order-process";
        final Long version = 1L;
        final Long processDefinitionKey = 67890L;
        final String tenantId = "tenant-1";

        // Save process instance
        persistence.save(processInstanceKey, businessKey, bpmnProcessId, version, processDefinitionKey, tenantId);

        // Capture the saved entity
        ArgumentCaptor<ProcessInstanceEntity> captor = ArgumentCaptor.forClass(ProcessInstanceEntity.class);
        verify(processInstanceRepository).save(captor.capture());

        // Verify the captured entity
        final var saved = captor.getValue();
        assertThat(saved.getProcessInstanceKey()).isEqualTo(processInstanceKey);
        assertThat(saved.getBusinessKey()).isEqualTo(businessKey);
        assertThat(saved.getBpmnProcessId()).isEqualTo(bpmnProcessId);
        assertThat(saved.getVersion()).isEqualTo(version);
        assertThat(saved.getProcessDefinitionKey()).isEqualTo(processDefinitionKey);
        assertThat(saved.getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void findById_existingKey_returnsProcessInstance() {
        // Set up mock
        final var processInstance = new ProcessInstanceEntity();
        processInstance.setProcessInstanceKey(12345L);
        processInstance.setBusinessKey("ORDER-123");
        when(processInstanceRepository.findById(12345L)).thenReturn(Optional.of(processInstance));

        // Find by ID
        final var result = persistence.findById(12345L);

        // Verify
        assertThat(result).isPresent();
        assertThat(result.get().getBusinessKey()).isEqualTo("ORDER-123");
        verify(processInstanceRepository).findById(12345L);
    }

    @Test
    void findById_nonExistentKey_returnsEmpty() {
        // Set up mock
        when(processInstanceRepository.findById(99999L)).thenReturn(Optional.empty());

        // Find by ID
        final var result = persistence.findById(99999L);

        // Verify
        assertThat(result).isEmpty();
        verify(processInstanceRepository).findById(99999L);
    }

    @Test
    void findByBusinessKey_existingKey_returnsProcessInstances() {
        // Set up mock
        final var processInstance1 = new ProcessInstanceEntity();
        processInstance1.setProcessInstanceKey(12345L);
        processInstance1.setBusinessKey("ORDER-123");

        final var processInstance2 = new ProcessInstanceEntity();
        processInstance2.setProcessInstanceKey(12346L);
        processInstance2.setBusinessKey("ORDER-123");

        when(processInstanceRepository.findByBusinessKey("ORDER-123"))
                .thenReturn(List.of(processInstance1, processInstance2));

        // Find by business key
        final var result = persistence.findByBusinessKey("ORDER-123");

        // Verify
        assertThat(result).hasSize(2);
        verify(processInstanceRepository).findByBusinessKey("ORDER-123");
    }

    @Test
    void findByBusinessKey_nonExistentKey_returnsEmptyList() {
        // Set up mock
        when(processInstanceRepository.findByBusinessKey("UNKNOWN")).thenReturn(List.of());

        // Find by business key
        final var result = persistence.findByBusinessKey("UNKNOWN");

        // Verify
        assertThat(result).isEmpty();
        verify(processInstanceRepository).findByBusinessKey("UNKNOWN");
    }

}
