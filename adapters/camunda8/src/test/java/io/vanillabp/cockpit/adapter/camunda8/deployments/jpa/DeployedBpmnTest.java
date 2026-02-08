package io.vanillabp.cockpit.adapter.camunda8.deployments.jpa;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeployedBpmnTest {

    @Test
    void constructor_createsInstance() {
        // Create instance
        final var deployedBpmn = new DeployedBpmn();

        // Should be created successfully
        assertThat(deployedBpmn).isNotNull();
    }

    @Test
    void type_hasBpmnValue() {
        // Verify the TYPE constant
        assertThat(DeployedBpmn.TYPE).isEqualTo("BPMN");
    }

    @Test
    void implementsDeployedBpmnInterface() {
        // Create instance
        final var deployedBpmn = new DeployedBpmn();

        // Should implement the interface
        assertThat(deployedBpmn).isInstanceOf(io.vanillabp.cockpit.adapter.camunda8.deployments.DeployedBpmn.class);
    }

    @Test
    void extendsDeploymentResource() {
        // Create instance
        final var deployedBpmn = new DeployedBpmn();

        // Should extend DeploymentResource
        assertThat(deployedBpmn).isInstanceOf(DeploymentResource.class);
    }

    @Test
    void inheritedMethods_workCorrectly() {
        // Create instance and use inherited methods
        final var deployedBpmn = new DeployedBpmn();
        deployedBpmn.setFileId(42);
        deployedBpmn.setResourceName("order-process.bpmn");
        deployedBpmn.setResource("<?xml?>".getBytes());

        // Verify inherited behavior
        assertThat(deployedBpmn.getFileId()).isEqualTo(42);
        assertThat(deployedBpmn.getResourceName()).isEqualTo("order-process.bpmn");
        assertThat(deployedBpmn.getResource()).isEqualTo("<?xml?>".getBytes());
    }

}
