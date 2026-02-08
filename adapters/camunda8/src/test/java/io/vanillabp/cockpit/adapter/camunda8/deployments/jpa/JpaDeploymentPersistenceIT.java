package io.vanillabp.cockpit.adapter.camunda8.deployments.jpa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for JpaDeploymentPersistence using an embedded H2 database.
 */
@SpringBootTest(classes = JpaTestConfiguration.class)
@AutoConfigureTestDatabase
@ActiveProfiles("test-jpa")
@Transactional
class JpaDeploymentPersistenceIT {

    @Autowired
    private DeploymentResourceRepository deploymentResourceRepository;

    @Autowired
    private DeploymentRepository deploymentRepository;

    @Autowired
    private DeployedBpmnRepository deployedBpmnRepository;

    private JpaDeploymentPersistence persistence;

    @BeforeEach
    void setUp() {
        deploymentRepository.deleteAll();
        deploymentResourceRepository.deleteAll();
        deployedBpmnRepository.deleteAll();

        persistence = new JpaDeploymentPersistence(
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
    void getBpmnNotOfPackage_returnsCorrectBpmns() {
        // Add first BPMN with deployment in package 100
        final var bpmn1 = persistence.addDeployedBpmn(30, "process1.bpmn", "test1".getBytes());
        persistence.addDeployedProcess(
                30001L, 1, 100, "order-module", "process1", bpmn1, OffsetDateTime.now());

        // Add second BPMN with deployment in package 200 (different package)
        final var bpmn2 = persistence.addDeployedBpmn(31, "process2.bpmn", "test2".getBytes());
        persistence.addDeployedProcess(
                30002L, 1, 200, "order-module", "process2", bpmn2, OffsetDateTime.now());

        // Add third BPMN with different workflow module
        final var bpmn3 = persistence.addDeployedBpmn(32, "process3.bpmn", "test3".getBytes());
        persistence.addDeployedProcess(
                30003L, 1, 50, "other-module", "process3", bpmn3, OffsetDateTime.now());

        // Get BPMNs not of package 100 for order-module
        final var result = persistence.getBpmnNotOfPackage("order-module", 100);

        // Should only return bpmn2 (order-module, package 200)
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFileId()).isEqualTo(31);
    }

    @Test
    void multipleDeploymentsOnSameBpmn_allTrackCorrectly() {
        // Create one BPMN with multiple deployments
        final var bpmn = persistence.addDeployedBpmn(40, "shared.bpmn", "shared content".getBytes());

        // Add multiple deployments (different versions)
        persistence.addDeployedProcess(40001L, 1, 100, "module1", "shared-process", bpmn, OffsetDateTime.now());
        persistence.addDeployedProcess(40002L, 2, 100, "module1", "shared-process", bpmn, OffsetDateTime.now());

        // Verify both deployments can be found
        assertThat(persistence.findDeployedProcess(40001L)).isPresent();
        assertThat(persistence.findDeployedProcess(40002L)).isPresent();
    }
}
