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
        // Test-Benutzer fuer die Properties-Instanz anlegen
        final var john = User.builder().id("john").firstName("John").lastName("Doe").build();
        final var jane = User.builder().id("jane").firstName("Jane").lastName("Doe").build();
        properties = new Properties(List.of(john, jane));
    }

    // --- getUser ---

    @Test
    void getUser_withExistingId_returnsMatchingUser() {
        // Benutzer mit bekannter ID suchen
        final var user = properties.getUser("john");

        // Der gefundene Benutzer muss die korrekte ID haben
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo("john");
        assertThat(user.getFirstName()).isEqualTo("John");
    }

    @Test
    void getUser_withNonExistingId_returnsNull() {
        // Benutzer mit unbekannter ID suchen
        final var user = properties.getUser("unknown");

        // Fuer unbekannte IDs muss null zurueckgegeben werden
        assertThat(user).isNull();
    }

    @Test
    void getUser_withNullId_returnsNull() {
        // Null-ID muss null zurueckgeben
        final var user = properties.getUser(null);

        assertThat(user).isNull();
    }

    @Test
    void getUser_returnsFirstMatchWhenMultipleUsersExist() {
        // Zweiten Benutzer mit derselben ID "jane" suchen
        final var user = properties.getUser("jane");

        // Der korrekte Benutzer muss gefunden werden
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo("jane");
        assertThat(user.getFirstName()).isEqualTo("Jane");
    }

    // --- getUsers ---

    @Test
    void getUsers_returnsConfiguredUserList() {
        // Gesamte Benutzerliste abrufen
        final var users = properties.getUsers();

        // Liste muss alle konfigurierten Benutzer enthalten
        assertThat(users).hasSize(2);
    }

}
