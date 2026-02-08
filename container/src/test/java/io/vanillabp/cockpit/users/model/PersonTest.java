package io.vanillabp.cockpit.users.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Person}.
 */
class PersonTest {

    @Test
    void settersAndGetters_workCorrectly() {
        // Arrange
        Person person = new Person();

        // Act
        person.setId("user-123");
        person.setFulltext("John Doe");
        person.setSort("doe-john");

        // Assert
        assertThat(person.getId()).isEqualTo("user-123");
        assertThat(person.getFulltext()).isEqualTo("John Doe");
        assertThat(person.getSort()).isEqualTo("doe-john");
    }

    @Test
    void newPerson_hasNullFields() {
        // Arrange
        Person person = new Person();

        // Assert
        assertThat(person.getId()).isNull();
        assertThat(person.getFulltext()).isNull();
        assertThat(person.getSort()).isNull();
    }

    @Test
    void setId_withNull_setsNull() {
        // Arrange
        Person person = new Person();
        person.setId("initial");

        // Act
        person.setId(null);

        // Assert
        assertThat(person.getId()).isNull();
    }
}
