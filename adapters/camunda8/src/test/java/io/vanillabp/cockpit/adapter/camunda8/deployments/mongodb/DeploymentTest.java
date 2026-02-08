package io.vanillabp.cockpit.adapter.camunda8.deployments.mongodb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DeploymentTest {

    // Concrete subclass for testing abstract Deployment
    private static class TestDeployment extends Deployment {
    }

    @Mock
    private DeploymentResource deploymentResource;

    private Deployment deployment;

    @BeforeEach
    void setUp() {
        deployment = new TestDeployment();
    }

    @Test
    void setAndGetDefinitionKey_storesAndReturnsValue() {
        // Set the definition key
        deployment.setDefinitionKey(2251799813685249L);

        // Verify retrieval
        assertThat(deployment.getDefinitionKey()).isEqualTo(2251799813685249L);
    }

    @Test
    void setAndGetVersion_storesAndReturnsValue() {
        // Set the version
        deployment.setVersion(3);

        // Verify retrieval
        assertThat(deployment.getVersion()).isEqualTo(3);
    }

    @Test
    void setAndGetRecordVersion_storesAndReturnsValue() {
        // Set the record version
        deployment.setRecordVersion(5);

        // Verify retrieval
        assertThat(deployment.getRecordVersion()).isEqualTo(5);
    }

    @Test
    void setAndGetPackageId_storesAndReturnsValue() {
        // Set the package ID
        deployment.setPackageId(42);

        // Verify retrieval
        assertThat(deployment.getPackageId()).isEqualTo(42);
    }

    @Test
    void setAndGetWorkflowModuleId_storesAndReturnsValue() {
        // Set the workflow module ID
        deployment.setWorkflowModuleId("order-module");

        // Verify retrieval
        assertThat(deployment.getWorkflowModuleId()).isEqualTo("order-module");
    }

    @Test
    void setAndGetDeployedResource_storesAndReturnsValue() {
        // Set the deployed resource
        deployment.setDeployedResource(deploymentResource);

        // Verify retrieval
        assertThat(deployment.getDeployedResource()).isSameAs(deploymentResource);
    }

    @Test
    void setAndGetPublishedAt_storesAndReturnsValue() {
        // Set the published timestamp
        final var publishedAt = OffsetDateTime.now();
        deployment.setPublishedAt(publishedAt);

        // Verify retrieval
        assertThat(deployment.getPublishedAt()).isEqualTo(publishedAt);
    }

    @Test
    void hashCode_returnsDefinitionKeyModulo() {
        // Set definition key
        deployment.setDefinitionKey(12345L);

        // HashCode should be definition key modulo Integer.MAX_VALUE
        assertThat(deployment.hashCode()).isEqualTo((int) (12345L % Integer.MAX_VALUE));
    }

    @Test
    void hashCode_withLargeKey_handlesOverflow() {
        // Set a large definition key
        deployment.setDefinitionKey(Long.MAX_VALUE);

        // The implementation casts to int first, then does modulo
        // (int) Long.MAX_VALUE = -1 due to overflow
        // -1 % Integer.MAX_VALUE = -1
        final int expectedHash = (int) Long.MAX_VALUE % Integer.MAX_VALUE;
        assertThat(deployment.hashCode()).isEqualTo(expectedHash);
    }

    @Test
    void equals_withSameDefinitionKey_returnsTrue() {
        // Create two deployments with same definition key
        deployment.setDefinitionKey(100L);

        final var other = new TestDeployment();
        other.setDefinitionKey(100L);

        // Should be equal
        assertThat(deployment.equals(other)).isTrue();
    }

    @Test
    void equals_withDifferentDefinitionKey_returnsFalse() {
        // Create two deployments with different definition keys
        deployment.setDefinitionKey(100L);

        final var other = new TestDeployment();
        other.setDefinitionKey(200L);

        // Should not be equal
        assertThat(deployment.equals(other)).isFalse();
    }

    @Test
    void equals_withNull_returnsFalse() {
        // Compare with null
        assertThat(deployment.equals(null)).isFalse();
    }

    @Test
    void equals_withDifferentType_returnsFalse() {
        // Compare with different type
        assertThat(deployment.equals("not a deployment")).isFalse();
    }

    @Test
    void equals_withSameInstance_returnsTrue() {
        // Compare with itself
        assertThat(deployment.equals(deployment)).isTrue();
    }

    @Test
    void collectionName_hasCorrectValue() {
        // Verify collection name constant
        assertThat(Deployment.COLLECTION_NAME).isEqualTo("CAMUNDA8_BC_DEPLOYMENTS");
    }

}
