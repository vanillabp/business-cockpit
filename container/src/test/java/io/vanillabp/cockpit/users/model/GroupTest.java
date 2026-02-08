package io.vanillabp.cockpit.users.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Group}.
 */
class GroupTest {

    @Test
    void settersAndGetters_workCorrectly() {
        // Arrange
        Group group = new Group();

        // Act
        group.setId("group-123");
        group.setFulltext("Administrators");
        group.setSort("admins");

        // Assert
        assertThat(group.getId()).isEqualTo("group-123");
        assertThat(group.getFulltext()).isEqualTo("Administrators");
        assertThat(group.getSort()).isEqualTo("admins");
    }

    @Test
    void newGroup_hasNullFields() {
        // Arrange
        Group group = new Group();

        // Assert
        assertThat(group.getId()).isNull();
        assertThat(group.getFulltext()).isNull();
        assertThat(group.getSort()).isNull();
    }

    @Test
    void setId_withNull_setsNull() {
        // Arrange
        Group group = new Group();
        group.setId("initial");

        // Act
        group.setId(null);

        // Assert
        assertThat(group.getId()).isNull();
    }
}
