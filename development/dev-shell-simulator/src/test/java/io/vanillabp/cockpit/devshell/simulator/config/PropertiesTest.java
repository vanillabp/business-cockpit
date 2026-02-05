package io.vanillabp.cockpit.devshell.simulator.config;

import io.vanillabp.cockpit.devshell.simulator.usermanagement.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PropertiesTest {

    private Properties properties;

    @BeforeEach
    void setUp() {
        // Create test users for the Properties instance
        final var john = User.builder().id("john").firstName("John").lastName("Doe").build();
        final var jane = User.builder().id("jane").firstName("Jane").lastName("Doe").build();
        properties = new Properties(List.of(john, jane));
    }

    // --- getUser ---

    @Test
    void getUser_withExistingId_returnsMatchingUser() {
        // Search for user with known ID
        final var user = properties.getUser("john");

        // The found user must have the correct ID
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo("john");
        assertThat(user.getFirstName()).isEqualTo("John");
    }

    @Test
    void getUser_withNonExistingId_returnsNull() {
        // Search for user with unknown ID
        final var user = properties.getUser("unknown");

        // For unknown IDs, null must be returned
        assertThat(user).isNull();
    }

    @Test
    void getUser_withNullId_returnsNull() {
        // Null ID must return null
        final var user = properties.getUser(null);

        assertThat(user).isNull();
    }

    @Test
    void getUser_returnsFirstMatchWhenMultipleUsersExist() {
        // Search for the second user with ID "jane"
        final var user = properties.getUser("jane");

        // The correct user must be found
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo("jane");
        assertThat(user.getFirstName()).isEqualTo("Jane");
    }

    // --- getUsers ---

    @Test
    void getUsers_returnsConfiguredUserList() {
        // Retrieve the complete user list
        final var users = properties.getUsers();

        // List must contain all configured users
        assertThat(users).hasSize(2);
    }

}
