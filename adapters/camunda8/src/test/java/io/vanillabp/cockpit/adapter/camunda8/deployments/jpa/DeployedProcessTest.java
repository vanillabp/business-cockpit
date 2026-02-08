package io.vanillabp.cockpit.adapter.camunda8.deployments.jpa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DeployedProcessTest {

    private DeployedProcess deployedProcess;

    @BeforeEach
    void setUp() {
        deployedProcess = new DeployedProcess();
    }

    @Test
    void constructor_createsInstance() {
        // Create instance
        final var process = new DeployedProcess();

        // Should be created successfully
        assertThat(process).isNotNull();
    }

    @Test
    void type_hasProcessValue() {
        // Verify the TYPE constant
        assertThat(DeployedProcess.TYPE).isEqualTo("PROCESS");
    }

    @Test
    void setAndGetBpmnProcessId_storesAndReturnsValue() {
        // Set the BPMN process ID
        deployedProcess.setBpmnProcessId("order-process");

        // Verify retrieval
        assertThat(deployedProcess.getBpmnProcessId()).isEqualTo("order-process");
    }

    @Test
    void implementsDeployedProcessInterface() {
        // Should implement the interface
        assertThat(deployedProcess).isInstanceOf(io.vanillabp.cockpit.adapter.camunda8.deployments.DeployedProcess.class);
    }

    @Test
    void extendsDeployment() {
        // Should extend Deployment
        assertThat(deployedProcess).isInstanceOf(Deployment.class);
    }

    @Test
    void inheritedMethods_workCorrectly() {
        // Use inherited methods from Deployment
        deployedProcess.setDefinitionKey(2251799813685249L);
        deployedProcess.setVersion(3);
        deployedProcess.setPackageId(42);
        deployedProcess.setWorkflowModuleId("order-module");
        deployedProcess.setPublishedAt(OffsetDateTime.now());

        // Verify inherited behavior
        assertThat(deployedProcess.getDefinitionKey()).isEqualTo(2251799813685249L);
        assertThat(deployedProcess.getVersion()).isEqualTo(3);
        assertThat(deployedProcess.getPackageId()).isEqualTo(42);
        assertThat(deployedProcess.getWorkflowModuleId()).isEqualTo("order-module");
        assertThat(deployedProcess.getPublishedAt()).isNotNull();
    }

    @Test
    void bpmnProcessId_isNullByDefault() {
        // Verify default value
        assertThat(deployedProcess.getBpmnProcessId()).isNull();
    }

    @Test
    void equals_useDefinitionKeyFromParent() {
        // Set up two processes with same definition key
        deployedProcess.setDefinitionKey(100L);
        deployedProcess.setBpmnProcessId("process-1");

        final var other = new DeployedProcess();
        other.setDefinitionKey(100L);
        other.setBpmnProcessId("process-2");

        // Should be equal based on definition key (inherited behavior)
        assertThat(deployedProcess.equals(other)).isTrue();
    }

    @Test
    void hashCode_usesDefinitionKeyFromParent() {
        // Set definition key
        deployedProcess.setDefinitionKey(12345L);

        // HashCode should be from parent's implementation
        assertThat(deployedProcess.hashCode()).isEqualTo((int) (12345L % Integer.MAX_VALUE));
    }

}
