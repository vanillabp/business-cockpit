package io.vanillabp.cockpit.adapter.camunda8.deployments;

import io.camunda.zeebe.client.api.response.Process;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeploymentServiceTest {

    @Mock
    private DeploymentPersistence persistence;

    @Mock
    private DeployedBpmn deployedBpmn;

    @Mock
    private DeployedProcess deployedProcess;

    @Mock
    private Process camundaProcess;

    private DeploymentService deploymentService;

    @BeforeEach
    void setUp() {
        deploymentService = new DeploymentService(persistence);
    }

    // --- ProcessInformation ---

    @Test
    void processInformation_storesAndReturnsValues() {
        // Create process information
        final var processInfo = new DeploymentService.ProcessInformation("my-process", 5);

        // Verify values are accessible
        assertThat(processInfo.getBpmnProcessId()).isEqualTo("my-process");
        assertThat(processInfo.getVersion()).isEqualTo(5);
    }

    // --- addBpmn ---

    @Test
    void addBpmn_withExistingResource_returnsExisting() {
        // Prepare existing resource using doReturn to avoid wildcard type issues
        doReturn(Optional.of(deployedBpmn)).when(persistence).findDeploymentResource(42);

        // Create minimal BPMN model
        final var model = createMinimalBpmnModel();

        // Add BPMN
        final var result = deploymentService.addBpmn(model, 42, "process.bpmn");

        // Should return existing resource
        assertThat(result).isSameAs(deployedBpmn);

        // Should not add new BPMN
        verify(persistence, never()).addDeployedBpmn(anyInt(), anyString(), any(byte[].class));
    }

    @Test
    void addBpmn_withNewResource_addsAndReturns() {
        // No existing resource
        doReturn(Optional.empty()).when(persistence).findDeploymentResource(42);

        // Prepare persistence to return new deployed BPMN
        doReturn(deployedBpmn).when(persistence).addDeployedBpmn(eq(42), eq("process.bpmn"), any(byte[].class));

        // Create minimal BPMN model
        final var model = createMinimalBpmnModel();

        // Add BPMN
        final var result = deploymentService.addBpmn(model, 42, "process.bpmn");

        // Should return new resource
        assertThat(result).isSameAs(deployedBpmn);

        // Should add new BPMN
        verify(persistence).addDeployedBpmn(eq(42), eq("process.bpmn"), any(byte[].class));
    }

    // --- recoverAddBpmn ---

    @Test
    void recoverAddBpmn_throwsRuntimeException() {
        // Prepare exception
        final var exception = new OptimisticLockingFailureException("Stale data");
        final var model = createMinimalBpmnModel();

        // Recovery should throw RuntimeException
        assertThatThrownBy(() ->
                deploymentService.recoverAddBpmn(exception, model, 42, "process.bpmn"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not save BPMN")
                .hasMessageContaining("process.bpmn")
                .hasCause(exception);
    }

    // --- addProcess ---

    @Test
    void addProcess_withNewProcess_addsAndCaches() {
        // No existing process
        doReturn(Optional.empty()).when(persistence).findDeployedProcess(12345L);

        // Prepare Camunda process
        when(camundaProcess.getProcessDefinitionKey()).thenReturn(12345L);
        when(camundaProcess.getVersion()).thenReturn(3);
        when(camundaProcess.getBpmnProcessId()).thenReturn("order-process");

        // Prepare deployed process to be returned
        when(deployedProcess.getBpmnProcessId()).thenReturn("order-process");
        when(deployedProcess.getVersion()).thenReturn(3);
        when(deployedProcess.getDefinitionKey()).thenReturn(12345L);

        doReturn(deployedProcess).when(persistence).addDeployedProcess(
                eq(12345L), eq(3), eq(1), eq("my-module"),
                eq("order-process"), eq(deployedBpmn), any());

        // Add process
        final var result = deploymentService.addProcess("my-module", 1, camundaProcess, deployedBpmn);

        // Should return deployed process
        assertThat(result).isSameAs(deployedProcess);

        // Process information should be cached
        final var cachedInfo = deploymentService.getProcessInformationByDefinitionKey(12345L);
        assertThat(cachedInfo).isPresent();
        assertThat(cachedInfo.get().getBpmnProcessId()).isEqualTo("order-process");
        assertThat(cachedInfo.get().getVersion()).isEqualTo(3L);
    }

    @Test
    void addProcess_withExistingProcessSameVersion_returnsExisting() {
        // Existing process with same version - use doReturn to avoid wildcard issues
        doReturn(Optional.of(deployedProcess)).when(persistence).findDeployedProcess(12345L);
        when(deployedProcess.getVersion()).thenReturn(3);
        when(deployedProcess.getWorkflowModuleId()).thenReturn("my-module");
        when(deployedProcess.getBpmnProcessId()).thenReturn("order-process");
        when(deployedProcess.getDefinitionKey()).thenReturn(12345L);

        // Prepare Camunda process
        when(camundaProcess.getProcessDefinitionKey()).thenReturn(12345L);
        when(camundaProcess.getVersion()).thenReturn(3);

        // Add process
        final var result = deploymentService.addProcess("my-module", 1, camundaProcess, deployedBpmn);

        // Should return existing process
        assertThat(result).isSameAs(deployedProcess);

        // Should not add new process
        verify(persistence, never()).addDeployedProcess(anyLong(), anyInt(), anyInt(),
                anyString(), anyString(), any(), any());
    }

    @Test
    void addProcess_withExistingProcessMissingWorkflowModuleId_updatesModuleId() {
        // Existing process with null workflow module ID - use doReturn
        doReturn(Optional.of(deployedProcess)).when(persistence).findDeployedProcess(12345L);
        when(deployedProcess.getVersion()).thenReturn(3);
        when(deployedProcess.getWorkflowModuleId()).thenReturn(null);
        when(deployedProcess.getBpmnProcessId()).thenReturn("order-process");
        when(deployedProcess.getDefinitionKey()).thenReturn(12345L);

        // Prepare Camunda process
        when(camundaProcess.getProcessDefinitionKey()).thenReturn(12345L);
        when(camundaProcess.getVersion()).thenReturn(3);

        // Prepare updated process - use doReturn
        doReturn(deployedProcess).when(persistence).updateMissingWorkflowModuleIdOfDeployedProcess(deployedProcess, "my-module");

        // Add process
        deploymentService.addProcess("my-module", 1, camundaProcess, deployedBpmn);

        // Should update and return process
        verify(persistence).updateMissingWorkflowModuleIdOfDeployedProcess(deployedProcess, "my-module");
    }

    // --- getProcessInformationByDefinitionKey ---

    @Test
    void getProcessInformationByDefinitionKey_withNoCachedInfo_returnsEmpty() {
        // No cached information
        final var result = deploymentService.getProcessInformationByDefinitionKey(99999L);

        // Should return empty
        assertThat(result).isEmpty();
    }

    // --- registerOldProcess ---

    @Test
    void registerOldProcess_cachesProcessInformation() {
        // Prepare deployed process
        when(deployedProcess.getBpmnProcessId()).thenReturn("legacy-process");
        when(deployedProcess.getVersion()).thenReturn(2);
        when(deployedProcess.getDefinitionKey()).thenReturn(54321L);

        // Register old process
        deploymentService.registerOldProcess(deployedProcess, deployedBpmn);

        // Process information should be cached
        final var cachedInfo = deploymentService.getProcessInformationByDefinitionKey(54321L);
        assertThat(cachedInfo).isPresent();
        assertThat(cachedInfo.get().getBpmnProcessId()).isEqualTo("legacy-process");
        assertThat(cachedInfo.get().getVersion()).isEqualTo(2L);
    }

    // --- getBpmnNotOfPackage ---

    @Test
    void getBpmnNotOfPackage_delegatesToPersistence() {
        // Prepare persistence to return list - use doReturn
        doReturn(Collections.singletonList(deployedBpmn)).when(persistence).getBpmnNotOfPackage("my-module", 5);

        // Get BPMN not of package
        final var result = deploymentService.getBpmnNotOfPackage("my-module", 5);

        // Should delegate to persistence
        assertThat(result).hasSize(1);
        verify(persistence).getBpmnNotOfPackage("my-module", 5);
    }

    // --- Helper methods ---

    private BpmnModelInstance createMinimalBpmnModel() {
        return Bpmn.createExecutableProcess("test-process")
                .startEvent()
                .endEvent()
                .done();
    }

}
