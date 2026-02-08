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

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MongoDbDeploymentPersistence using Testcontainers MongoDB.
 */
@SpringBootTest(classes = MongoDbTestConfiguration.class)
@Testcontainers
@ActiveProfiles("test-mongodb")
class MongoDbDeploymentPersistenceIT {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private DeploymentResourceRepository deploymentResourceRepository;

    @Autowired
    private DeploymentRepository deploymentRepository;

    @Autowired
    private DeployedBpmnRepository deployedBpmnRepository;

    private MongoDbDeploymentPersistence persistence;

    @BeforeEach
    void setUp() {
        // Clear data before each test
        deploymentResourceRepository.deleteAll();
        deploymentRepository.deleteAll();
        deployedBpmnRepository.deleteAll();

        persistence = new MongoDbDeploymentPersistence(
                deploymentResourceRepository,
                deploymentRepository,
                deployedBpmnRepository);
    }

    @Test
    void addDeployedBpmn_persistsBpmnResource() {
        // Add a BPMN resource
        final var bpmnContent = "<?xml version=\"1.0\"?>".getBytes();
        final var result = persistence.addDeployedBpmn(1, "order-process.bpmn", bpmnContent);

        // Verify it was persisted
        assertThat(result).isNotNull();
        assertThat(result.getFileId()).isEqualTo(1);
        assertThat(result.getResourceName()).isEqualTo("order-process.bpmn");
        assertThat(result.getResource()).isEqualTo(bpmnContent);

        // Verify it can be retrieved
        final var found = persistence.findDeploymentResource(1);
        assertThat(found).isPresent();
        assertThat(found.get().getResourceName()).isEqualTo("order-process.bpmn");
    }

    @Test
    void addDeployedProcess_persistsDeploymentWithBpmn() {
        // First add a BPMN resource
        final var bpmnContent = "<?xml version=\"1.0\"?>".getBytes();
        final var bpmn = persistence.addDeployedBpmn(10, "order-process.bpmn", bpmnContent);

        // Add a deployed process
        final var result = persistence.addDeployedProcess(
                12345L,
                1,
                100,
                "order-module",
                "order-process",
                bpmn,
                OffsetDateTime.now());

        // Verify the deployment
        assertThat(result).isNotNull();
        assertThat(result.getDefinitionKey()).isEqualTo(12345L);
        assertThat(result.getVersion()).isEqualTo(1);
        assertThat(result.getPackageId()).isEqualTo(100);
        assertThat(result.getWorkflowModuleId()).isEqualTo("order-module");
        assertThat(((DeployedProcess) result).getBpmnProcessId()).isEqualTo("order-process");

        // Verify it can be found by definition key
        final var found = persistence.findDeployedProcess(12345L);
        assertThat(found).isPresent();
        assertThat(((DeployedProcess) found.get()).getBpmnProcessId()).isEqualTo("order-process");
    }

    @Test
    void findDeployedProcess_returnsEmptyForNonExistent() {
        // Try to find a non-existent deployment
        final var result = persistence.findDeployedProcess(99999L);

        // Verify empty result
        assertThat(result).isEmpty();
    }

    @Test
    void updateMissingWorkflowModuleIdOfDeployedProcess_updatesAndSaves() {
        // Add a BPMN and deployment without workflow module ID
        final var bpmn = persistence.addDeployedBpmn(20, "test.bpmn", "test".getBytes());
        final var deployment = persistence.addDeployedProcess(
                22222L, 1, 200, null, "test-process", bpmn, OffsetDateTime.now());

        // Update the workflow module ID
        final var updated = persistence.updateMissingWorkflowModuleIdOfDeployedProcess(
                (DeployedProcess) deployment, "test-module");

        // Verify the update
        assertThat(updated.getWorkflowModuleId()).isEqualTo("test-module");

        // Verify persistence
        final var found = persistence.findDeployedProcess(22222L);
        assertThat(found).isPresent();
        assertThat(found.get().getWorkflowModuleId()).isEqualTo("test-module");
    }

    @Test
    void multipleDeploymentsOnSameBpmn_allProcessesCanBeFound() {
        // Create one BPMN with multiple deployments
        final var bpmn = persistence.addDeployedBpmn(40, "shared.bpmn", "shared content".getBytes());

        // Add multiple deployments (different versions)
        persistence.addDeployedProcess(40001L, 1, 100, "module1", "shared-process", bpmn, OffsetDateTime.now());
        persistence.addDeployedProcess(40002L, 2, 100, "module1", "shared-process", bpmn, OffsetDateTime.now());

        // Verify both deployments can be found independently
        final var found1 = persistence.findDeployedProcess(40001L);
        assertThat(found1).isPresent();
        assertThat(found1.get().getVersion()).isEqualTo(1);

        final var found2 = persistence.findDeployedProcess(40002L);
        assertThat(found2).isPresent();
        assertThat(found2.get().getVersion()).isEqualTo(2);
    }
}
