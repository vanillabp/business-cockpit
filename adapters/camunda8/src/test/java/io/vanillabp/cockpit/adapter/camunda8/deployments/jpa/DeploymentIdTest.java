package io.vanillabp.cockpit.adapter.camunda8.deployments.jpa;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeploymentIdTest {

    @Test
    void defaultConstructor_createsEmptyInstance() {
        // Default constructor for JPA
        final var id = new DeploymentId();
        assertThat(id).isNotNull();
    }

    @Test
    void constructor_withParameters_setsValues() {
        // Constructor with definition key and version
        final var id = new DeploymentId(12345L, 3);
        assertThat(id).isNotNull();
    }

    @Test
    void hashCode_sameValues_returnsSameHash() {
        // Two IDs with same values
        final var id1 = new DeploymentId(12345L, 3);
        final var id2 = new DeploymentId(12345L, 3);

        // Hash codes should be equal
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    void hashCode_differentDefinitionKey_returnsDifferentHash() {
        // Two IDs with different definition keys
        final var id1 = new DeploymentId(12345L, 3);
        final var id2 = new DeploymentId(67890L, 3);

        // Hash codes should be different
        assertThat(id1.hashCode()).isNotEqualTo(id2.hashCode());
    }

    @Test
    void hashCode_differentVersion_returnsDifferentHash() {
        // Two IDs with different versions
        final var id1 = new DeploymentId(12345L, 3);
        final var id2 = new DeploymentId(12345L, 5);

        // Hash codes should be different
        assertThat(id1.hashCode()).isNotEqualTo(id2.hashCode());
    }

    @Test
    void equals_sameValues_returnsTrue() {
        // Two IDs with same values
        final var id1 = new DeploymentId(12345L, 3);
        final var id2 = new DeploymentId(12345L, 3);

        // Should be equal
        assertThat(id1.equals(id2)).isTrue();
    }

    @Test
    void equals_differentDefinitionKey_returnsFalse() {
        // Two IDs with different definition keys
        final var id1 = new DeploymentId(12345L, 3);
        final var id2 = new DeploymentId(67890L, 3);

        // Should not be equal
        assertThat(id1.equals(id2)).isFalse();
    }

    @Test
    void equals_differentVersion_returnsFalse() {
        // Two IDs with different versions
        final var id1 = new DeploymentId(12345L, 3);
        final var id2 = new DeploymentId(12345L, 5);

        // Should not be equal
        assertThat(id1.equals(id2)).isFalse();
    }

    @Test
    void equals_null_returnsFalse() {
        // Compare with null
        final var id = new DeploymentId(12345L, 3);

        // Should not be equal to null
        assertThat(id.equals(null)).isFalse();
    }

    @Test
    void equals_differentClass_returnsFalse() {
        // Compare with different class
        final var id = new DeploymentId(12345L, 3);

        // Should not be equal to different class
        assertThat(id.equals("not a DeploymentId")).isFalse();
    }

    @Test
    void equals_sameInstance_returnsTrue() {
        // Compare with same instance
        final var id = new DeploymentId(12345L, 3);

        // Should be equal to itself
        assertThat(id.equals(id)).isTrue();
    }

}
