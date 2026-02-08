package io.vanillabp.cockpit.adapter.camunda8.deployments.jpa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeploymentResourceTest {

    // Concrete subclass for testing abstract DeploymentResource
    private static class TestDeploymentResource extends DeploymentResource {
    }

    private DeploymentResource resource;

    @BeforeEach
    void setUp() {
        resource = new TestDeploymentResource();
    }

    @Test
    void setAndGetFileId_storesAndReturnsValue() {
        // Set the file ID
        resource.setFileId(42);

        // Verify retrieval
        assertThat(resource.getFileId()).isEqualTo(42);
    }

    @Test
    void setAndGetRecordVersion_storesAndReturnsValue() {
        // Set the record version
        resource.setRecordVersion(3);

        // Verify retrieval
        assertThat(resource.getRecordVersion()).isEqualTo(3);
    }

    @Test
    void setAndGetResourceName_storesAndReturnsValue() {
        // Set the resource name
        resource.setResourceName("order-process.bpmn");

        // Verify retrieval
        assertThat(resource.getResourceName()).isEqualTo("order-process.bpmn");
    }

    @Test
    void setAndGetResource_storesAndReturnsValue() {
        // Set the resource bytes
        final var resourceBytes = "<?xml version='1.0'?>".getBytes();
        resource.setResource(resourceBytes);

        // Verify retrieval
        assertThat(resource.getResource()).isEqualTo(resourceBytes);
    }

    @Test
    void setAndGetDeployments_storesAndReturnsValue() {
        // Create deployment list
        final List<Deployment> deployments = Arrays.asList();
        resource.setDeployments(deployments);

        // Verify retrieval
        assertThat(resource.getDeployments()).isSameAs(deployments);
    }

    @Test
    void hashCode_returnsFileId() {
        // Set file ID
        resource.setFileId(12345);

        // HashCode should equal file ID
        assertThat(resource.hashCode()).isEqualTo(12345);
    }

    @Test
    void equals_withSameFileId_returnsTrue() {
        // Create two resources with same file ID
        resource.setFileId(100);

        final var other = new TestDeploymentResource();
        other.setFileId(100);

        // Should be equal
        assertThat(resource.equals(other)).isTrue();
    }

    @Test
    void equals_withDifferentFileId_returnsFalse() {
        // Create two resources with different file IDs
        resource.setFileId(100);

        final var other = new TestDeploymentResource();
        other.setFileId(200);

        // Should not be equal
        assertThat(resource.equals(other)).isFalse();
    }

    @Test
    void equals_withNull_returnsFalse() {
        // Compare with null
        assertThat(resource.equals(null)).isFalse();
    }

    @Test
    void equals_withDifferentType_returnsFalse() {
        // Compare with different type
        assertThat(resource.equals("not a deployment resource")).isFalse();
    }

    @Test
    void equals_withSameInstance_returnsTrue() {
        // Compare with itself
        assertThat(resource.equals(resource)).isTrue();
    }

}
