package io.vanillabp.cockpit.adapter.camunda8.deployments.mongodb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MongoDbProcessInstancePersistence using Testcontainers MongoDB.
 */
@SpringBootTest(classes = MongoDbTestConfiguration.class)
@Testcontainers
@ActiveProfiles("test-mongodb")
class MongoDbProcessInstancePersistenceIT {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    private MongoDbProcessInstancePersistence persistence;

    @BeforeEach
    void setUp() {
        // Clear data before each test
        processInstanceRepository.deleteAll();

        persistence = new MongoDbProcessInstancePersistence(processInstanceRepository);
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

    @Test
    void save_overwritesExistingInstance() {
        // Save initial instance
        persistence.save(60001L, "ORDER-INITIAL", "process1", 1L, 70001L, "tenant-1");

        // Save again with same key but different data
        persistence.save(60001L, "ORDER-UPDATED", "process2", 2L, 70002L, "tenant-2");

        // Verify the update
        final var result = persistence.findById(60001L);
        assertThat(result).isPresent();
        assertThat(result.get().getBusinessKey()).isEqualTo("ORDER-UPDATED");
        assertThat(result.get().getBpmnProcessId()).isEqualTo("process2");
        assertThat(result.get().getVersion()).isEqualTo(2L);
    }
}
