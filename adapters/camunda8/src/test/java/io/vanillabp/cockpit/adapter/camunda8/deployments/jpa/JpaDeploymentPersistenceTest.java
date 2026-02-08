package io.vanillabp.cockpit.adapter.camunda8.deployments.jpa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for JpaDeploymentPersistence.
 */
@ExtendWith(MockitoExtension.class)
class JpaDeploymentPersistenceTest {

    @Mock
    private DeploymentResourceRepository deploymentResourceRepository;

    @Mock
    private DeploymentRepository deploymentRepository;

    @Mock
    private DeployedBpmnRepository deployedBpmnRepository;

    private JpaDeploymentPersistence persistence;

    @BeforeEach
    void setUp() {
        persistence = new JpaDeploymentPersistence(
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
    void addDeployedProcess_withJpaDeployedBpmn_savesSuccessfully() {
        // Set up deployed BPMN
        final var bpmn = new DeployedBpmn();
        bpmn.setFileId(42);
        bpmn.setResourceName("order-process.bpmn");

        // Mock repository save
        when(deploymentRepository.save(any(DeployedProcess.class))).thenAnswer(inv -> inv.getArgument(0));

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
    }

    @Test
    void addDeployedProcess_withNonJpaDeployedBpmn_loadsFromRepository() {
        // Set up a non-JPA deployed BPMN (using the interface directly)
        final io.vanillabp.cockpit.adapter.camunda8.deployments.DeployedBpmn bpmn =
                new io.vanillabp.cockpit.adapter.camunda8.deployments.DeployedBpmn() {
            @Override
            public int getFileId() { return 42; }
            @Override
            public String getResourceName() { return "test.bpmn"; }
            @Override
            public byte[] getResource() { return new byte[0]; }
            @Override
            public <D extends io.vanillabp.cockpit.adapter.camunda8.deployments.Deployment> java.util.List<D> getDeployments() { return java.util.List.of(); }
        };

        // Set up mock for repository lookup
        final var jpaBpmn = new DeployedBpmn();
        jpaBpmn.setFileId(42);
        when(deploymentResourceRepository.findById(42)).thenReturn(Optional.of(jpaBpmn));
        when(deploymentRepository.save(any(DeployedProcess.class))).thenAnswer(inv -> inv.getArgument(0));

        // Add deployed process
        final var result = persistence.addDeployedProcess(
                12345L,
                1,
                100,
                "order-module",
                "order-process",
                bpmn,
                OffsetDateTime.now());

        // Verify that repository was queried
        verify(deploymentResourceRepository).findById(42);
        assertThat((DeployedBpmn) result.getDeployedResource()).isSameAs(jpaBpmn);
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
    void getBpmnNotOfPackage_returnsDistinctBpmns() {
        // Set up mock list
        final var bpmn1 = new DeployedBpmn();
        bpmn1.setFileId(1);
        final var bpmn2 = new DeployedBpmn();
        bpmn2.setFileId(2);
        when(deployedBpmnRepository.findByDeployments_workflowModuleIdAndDeployments_PackageIdNot(
                "order-module", 100)).thenReturn(List.of(bpmn1, bpmn2, bpmn1)); // Include duplicate

        // Get BPMNs not of package
        final var result = persistence.getBpmnNotOfPackage("order-module", 100);

        // Verify distinct results
        assertThat(result).hasSize(2);
        verify(deployedBpmnRepository).findByDeployments_workflowModuleIdAndDeployments_PackageIdNot(
                "order-module", 100);
    }

    @Test
    void getBpmnNotOfPackage_emptyResult_returnsEmptyList() {
        // Set up mock
        when(deployedBpmnRepository.findByDeployments_workflowModuleIdAndDeployments_PackageIdNot(
                "order-module", 100)).thenReturn(List.of());

        // Get BPMNs not of package
        final var result = persistence.getBpmnNotOfPackage("order-module", 100);

        // Verify
        assertThat(result).isEmpty();
    }

}
