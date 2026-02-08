package io.vanillabp.cockpit.adapter.camunda8.deployments.mongodb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MongoDbDeploymentPersistence.
 */
@ExtendWith(MockitoExtension.class)
class MongoDbDeploymentPersistenceTest {

    @Mock
    private DeploymentResourceRepository deploymentResourceRepository;

    @Mock
    private DeploymentRepository deploymentRepository;

    @Mock
    private DeployedBpmnRepository deployedBpmnRepository;

    private MongoDbDeploymentPersistence persistence;

    @BeforeEach
    void setUp() {
        persistence = new MongoDbDeploymentPersistence(
                deploymentResourceRepository,
                deploymentRepository,
                deployedBpmnRepository);
    }

    @Test
    void findDeployedProcess_existingKey_returnsDeployment() {
        // Set up mock deployment
        final var deployment = new DeployedProcess();
        deployment.setDefinitionKey(12345L);
        deployment.setWorkflowModuleId("order-module");
        when(deploymentRepository.findByDefinitionKey(12345L)).thenReturn(Optional.of(deployment));

        // Find deployed process
        final var result = persistence.findDeployedProcess(12345L);

        // Verify
        assertThat(result).isPresent();
        assertThat(result.get().getWorkflowModuleId()).isEqualTo("order-module");
        verify(deploymentRepository).findByDefinitionKey(12345L);
    }

    @Test
    void findDeployedProcess_nonExistentKey_returnsEmpty() {
        // Set up mock
        when(deploymentRepository.findByDefinitionKey(99999L)).thenReturn(Optional.empty());

        // Find deployed process
        final var result = persistence.findDeployedProcess(99999L);

        // Verify
        assertThat(result).isEmpty();
        verify(deploymentRepository).findByDefinitionKey(99999L);
    }

    @Test
    void updateMissingWorkflowModuleIdOfDeployedProcess_updatesAndSaves() {
        // Set up deployed process
        final var deployedProcess = new DeployedProcess();
        deployedProcess.setDefinitionKey(12345L);
        when(deploymentRepository.save(any(DeployedProcess.class))).thenAnswer(inv -> inv.getArgument(0));

        // Update workflow module ID
        final var result = persistence.updateMissingWorkflowModuleIdOfDeployedProcess(
                deployedProcess, "order-module");

        // Verify
        assertThat(result.getWorkflowModuleId()).isEqualTo("order-module");
        verify(deploymentRepository).save(deployedProcess);
    }

    @Test
    void addDeployedProcess_withMongoDeployedBpmn_savesSuccessfully() {
        // Set up deployed BPMN with existing deployments list
        final var bpmn = new DeployedBpmn();
        bpmn.setFileId(42);
        bpmn.setResourceName("order-process.bpmn");
        bpmn.setDeployments(new ArrayList<>());

        // Mock repository saves
        when(deploymentRepository.save(any(DeployedProcess.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deploymentResourceRepository.save(any(DeployedBpmn.class))).thenAnswer(inv -> inv.getArgument(0));

        // Add deployed process
        final var result = persistence.addDeployedProcess(
                12345L,
                1,
                100,
                "order-module",
                "order-process",
                bpmn,
                OffsetDateTime.now());

        // Verify
        assertThat(result).isNotNull();
        assertThat(result.getDefinitionKey()).isEqualTo(12345L);
        assertThat(result.getVersion()).isEqualTo(1);
        assertThat(result.getPackageId()).isEqualTo(100);
        assertThat(result.getWorkflowModuleId()).isEqualTo("order-module");
        assertThat(result.getBpmnProcessId()).isEqualTo("order-process");
        assertThat((DeployedBpmn) result.getDeployedResource()).isSameAs(bpmn);
        verify(deploymentRepository).save(any(DeployedProcess.class));
        verify(deploymentResourceRepository).save(bpmn);
    }

    @Test
    void addDeployedProcess_withMongoDeployedBpmn_nullDeployments_createsNewList() {
        // Set up deployed BPMN with null deployments
        final var bpmn = new DeployedBpmn();
        bpmn.setFileId(42);
        bpmn.setResourceName("order-process.bpmn");
        bpmn.setDeployments(null);

        // Mock repository saves
        when(deploymentRepository.save(any(DeployedProcess.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deploymentResourceRepository.save(any(DeployedBpmn.class))).thenAnswer(inv -> inv.getArgument(0));

        // Add deployed process
        final var result = persistence.addDeployedProcess(
                12345L,
                1,
                100,
                "order-module",
                "order-process",
                bpmn,
                OffsetDateTime.now());

        // Verify
        assertThat(result).isNotNull();
        verify(deploymentResourceRepository).save(bpmn);
    }

    @Test
    void addDeployedProcess_withNonMongoDeployedBpmn_loadsFromRepository() {
        // Set up a non-MongoDB deployed BPMN (using an anonymous class)
        final var nonMongoBpmn = new TestDeployedBpmn(42, "test.bpmn", new byte[0]);

        // Set up mock for repository lookup
        final var mongoBpmn = new DeployedBpmn();
        mongoBpmn.setFileId(42);
        mongoBpmn.setDeployments(new ArrayList<>());
        when(deploymentResourceRepository.findById(42)).thenReturn(Optional.of(mongoBpmn));
        when(deploymentRepository.save(any(DeployedProcess.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deploymentResourceRepository.save(any(DeployedBpmn.class))).thenAnswer(inv -> inv.getArgument(0));

        // Add deployed process
        final var result = persistence.addDeployedProcess(
                12345L,
                1,
                100,
                "order-module",
                "order-process",
                nonMongoBpmn,
                OffsetDateTime.now());

        // Verify that repository was queried
        verify(deploymentResourceRepository).findById(42);
        assertThat((DeployedBpmn) result.getDeployedResource()).isSameAs(mongoBpmn);
    }

    @Test
    void addDeployedProcess_withNonMongoDeployedBpmn_notFound_throwsException() {
        // Set up a non-MongoDB deployed BPMN
        final var nonMongoBpmn = new TestDeployedBpmn(999, "test.bpmn", new byte[0]);

        // Set up mock for repository lookup - not found
        when(deploymentResourceRepository.findById(999)).thenReturn(Optional.empty());

        // Verify exception is thrown
        assertThatThrownBy(() -> persistence.addDeployedProcess(
                12345L, 1, 100, "order-module", "order-process", nonMongoBpmn, OffsetDateTime.now()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unknown BPMN file ID: 999");
    }

    @Test
    void findDeploymentResource_existingId_returnsResource() {
        // Set up mock resource
        final var resource = new DeployedBpmn();
        resource.setFileId(42);
        resource.setResourceName("order-process.bpmn");
        when(deploymentResourceRepository.findById(42)).thenReturn(Optional.of(resource));

        // Find deployment resource
        final var result = persistence.findDeploymentResource(42);

        // Verify
        assertThat(result).isPresent();
        assertThat(result.get().getResourceName()).isEqualTo("order-process.bpmn");
        verify(deploymentResourceRepository).findById(42);
    }

    @Test
    void findDeploymentResource_nonExistentId_returnsEmpty() {
        // Set up mock
        when(deploymentResourceRepository.findById(99999)).thenReturn(Optional.empty());

        // Find deployment resource
        final var result = persistence.findDeploymentResource(99999);

        // Verify
        assertThat(result).isEmpty();
        verify(deploymentResourceRepository).findById(99999);
    }

    @Test
    void addDeployedBpmn_savesNewBpmn() {
        // Set up mock
        when(deploymentResourceRepository.save(any(DeployedBpmn.class))).thenAnswer(inv -> inv.getArgument(0));

        // Add deployed BPMN
        final var bpmnContent = "<?xml?>".getBytes();
        final var result = persistence.addDeployedBpmn(42, "order-process.bpmn", bpmnContent);

        // Verify
        assertThat(result).isNotNull();
        assertThat(result.getFileId()).isEqualTo(42);
        assertThat(result.getResourceName()).isEqualTo("order-process.bpmn");
        assertThat(result.getResource()).isEqualTo(bpmnContent);
        verify(deploymentResourceRepository).save(any(DeployedBpmn.class));
    }

    @Test
    void getBpmnNotOfPackage_matchingWorkflowModule_returnsBpmns() {
        // Set up deployment with matching workflow module
        final var deployment1 = new DeployedProcess();
        deployment1.setWorkflowModuleId("order-module");
        deployment1.setPackageId(50); // Different package ID

        final var bpmn1 = new DeployedBpmn();
        bpmn1.setFileId(1);
        bpmn1.setDeployments(List.of(deployment1));

        final var deployment2 = new DeployedProcess();
        deployment2.setWorkflowModuleId("order-module");
        deployment2.setPackageId(100); // Same package ID

        final var bpmn2 = new DeployedBpmn();
        bpmn2.setFileId(2);
        bpmn2.setDeployments(List.of(deployment2));

        when(deployedBpmnRepository.findAll()).thenReturn(List.of(bpmn1, bpmn2));

        // Get BPMNs not of package
        final var result = persistence.getBpmnNotOfPackage("order-module", 100);

        // Verify only bpmn1 is returned (different package ID)
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFileId()).isEqualTo(1);
    }

    @Test
    void getBpmnNotOfPackage_differentWorkflowModule_returnsEmpty() {
        // Set up deployment with different workflow module
        final var deployment = new DeployedProcess();
        deployment.setWorkflowModuleId("other-module");
        deployment.setPackageId(50);

        final var bpmn = new DeployedBpmn();
        bpmn.setFileId(1);
        bpmn.setDeployments(List.of(deployment));

        when(deployedBpmnRepository.findAll()).thenReturn(List.of(bpmn));

        // Get BPMNs not of package
        final var result = persistence.getBpmnNotOfPackage("order-module", 100);

        // Verify empty result
        assertThat(result).isEmpty();
    }

    @Test
    void getBpmnNotOfPackage_nullWorkflowModuleId_returnsEmpty() {
        // Set up deployment with null workflow module ID
        final var deployment = new DeployedProcess();
        deployment.setWorkflowModuleId(null);
        deployment.setPackageId(50);

        final var bpmn = new DeployedBpmn();
        bpmn.setFileId(1);
        bpmn.setDeployments(List.of(deployment));

        when(deployedBpmnRepository.findAll()).thenReturn(List.of(bpmn));

        // Get BPMNs not of package
        final var result = persistence.getBpmnNotOfPackage("order-module", 100);

        // Verify empty result (null workflow module IDs should not match)
        assertThat(result).isEmpty();
    }

    @Test
    void getBpmnNotOfPackage_emptyRepository_returnsEmptyList() {
        // Set up mock
        when(deployedBpmnRepository.findAll()).thenReturn(List.of());

        // Get BPMNs not of package
        final var result = persistence.getBpmnNotOfPackage("order-module", 100);

        // Verify
        assertThat(result).isEmpty();
    }

    /**
     * Test implementation of DeployedBpmn interface for testing non-MongoDB objects.
     */
    private static class TestDeployedBpmn implements io.vanillabp.cockpit.adapter.camunda8.deployments.DeployedBpmn {
        private final int fileId;
        private final String resourceName;
        private final byte[] resource;

        TestDeployedBpmn(int fileId, String resourceName, byte[] resource) {
            this.fileId = fileId;
            this.resourceName = resourceName;
            this.resource = resource;
        }

        @Override
        public int getFileId() { return fileId; }

        @Override
        public String getResourceName() { return resourceName; }

        @Override
        public byte[] getResource() { return resource; }

        @Override
        public <D extends io.vanillabp.cockpit.adapter.camunda8.deployments.Deployment> java.util.List<D> getDeployments() { return java.util.List.of(); }
    }

}
