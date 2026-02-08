package io.vanillabp.cockpit.adapter.camunda8.deployments.jpa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for JpaProcessInstancePersistence using an embedded H2 database.
 */
@SpringBootTest(classes = JpaTestConfiguration.class)
@AutoConfigureTestDatabase
@ActiveProfiles("test-jpa")
@Transactional
class JpaProcessInstancePersistenceIT {

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    private JpaProcessInstancePersistence persistence;

    @BeforeEach
    void setUp() {
        processInstanceRepository.deleteAll();
        persistence = new JpaProcessInstancePersistence(processInstanceRepository);
    }

    @Test
    void save_persistsProcessInstance() {
        // Save a process instance
        persistence.save(12345L, "ORDER-123", "order-process", 1L, 67890L, "tenant-1");

        // Verify it was persisted
        final var result = persistence.findById(12345L);
        assertThat(result).isPresent();
        assertThat(result.get().getProcessInstanceKey()).isEqualTo(12345L);
        assertThat(result.get().getBusinessKey()).isEqualTo("ORDER-123");
        assertThat(result.get().getBpmnProcessId()).isEqualTo("order-process");
        assertThat(result.get().getVersion()).isEqualTo(1L);
        assertThat(result.get().getProcessDefinitionKey()).isEqualTo(67890L);
        assertThat(result.get().getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    void findById_nonExistent_returnsEmpty() {
        // Try to find a non-existent process instance
        final var result = persistence.findById(99999L);

        // Verify empty result
        assertThat(result).isEmpty();
    }

    @Test
    void findByBusinessKey_returnsMatchingInstances() {
        // Save multiple process instances with same business key
        persistence.save(10001L, "ORDER-ABC", "process1", 1L, 100L, "tenant");
        persistence.save(10002L, "ORDER-ABC", "process2", 1L, 101L, "tenant");
        persistence.save(10003L, "ORDER-XYZ", "process3", 1L, 102L, "tenant");

        // Find by business key
        final var result = persistence.findByBusinessKey("ORDER-ABC");

        // Verify correct instances returned
        assertThat(result).hasSize(2);
        assertThat(result).extracting(io.vanillabp.cockpit.adapter.camunda8.deployments.ProcessInstance::getProcessInstanceKey)
                .containsExactlyInAnyOrder(10001L, 10002L);
    }

    @Test
    void findByBusinessKey_nonExistent_returnsEmpty() {
        // Add some data
        persistence.save(20001L, "EXISTING", "process", 1L, 100L, "tenant");

        // Find by non-existent business key
        final var result = persistence.findByBusinessKey("NON-EXISTENT");

        // Verify empty result
        assertThat(result).isEmpty();
    }

    @Test
    void save_multipleInstances_allPersisted() {
        // Save multiple distinct instances
        for (int i = 1; i <= 10; i++) {
            persistence.save(
                    30000L + i,
                    "BATCH-" + i,
                    "batch-process",
                    1L,
                    40000L + i,
                    "tenant-1"
            );
        }

        // Verify all were persisted
        for (int i = 1; i <= 10; i++) {
            final var result = persistence.findById(30000L + i);
            assertThat(result).isPresent();
            assertThat(result.get().getBusinessKey()).isEqualTo("BATCH-" + i);
        }
    }

    @Test
    void save_nullTenantId_persistsSuccessfully() {
        // Save with null tenant ID
        persistence.save(50001L, "ORDER-NULL", "order-process", 1L, 60001L, null);

        // Verify it was persisted
        final var result = persistence.findById(50001L);
        assertThat(result).isPresent();
        assertThat(result.get().getTenantId()).isNull();
    }
}
