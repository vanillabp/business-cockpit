package io.vanillabp.cockpit.commons.rest.adapter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakAuthenticationTest {

    @Test
    void extendsClient() {
        KeycloakAuthentication keycloak = new KeycloakAuthentication();

        assertThat(keycloak).isInstanceOf(Client.class);
    }

    @Test
    void defaultValues_areSetCorrectly() {
        KeycloakAuthentication keycloak = new KeycloakAuthentication();

        assertThat(keycloak.getClientId()).isNull();
        // Inherited from Client
        assertThat(keycloak.getBaseUrl()).isEqualTo(Client.TO_BE_DEFINED_URL);
    }

    @Test
    void setClientId_updatesValue() {
        KeycloakAuthentication keycloak = new KeycloakAuthentication();
        keycloak.setClientId("keycloak-client");

        assertThat(keycloak.getClientId()).isEqualTo("keycloak-client");
    }

    @Test
    void fullConfiguration_worksCorrectly() {
        KeycloakAuthentication keycloak = new KeycloakAuthentication();
        keycloak.setBaseUrl("https://keycloak.example.com/auth/realms/myrealm");
        keycloak.setClientId("my-keycloak-client");
        keycloak.setConnectTimeout(3000);
        keycloak.setReadTimeout(15000);

        assertThat(keycloak.isInitialized()).isTrue();
        assertThat(keycloak.getBaseUrl()).isEqualTo("https://keycloak.example.com/auth/realms/myrealm");
        assertThat(keycloak.getClientId()).isEqualTo("my-keycloak-client");
        assertThat(keycloak.getConnectTimeout()).isEqualTo(3000);
        assertThat(keycloak.getReadTimeout()).isEqualTo(15000);
    }

    @Test
    void inheritedMethods_fromClient_workCorrectly() {
        KeycloakAuthentication keycloak = new KeycloakAuthentication();
        keycloak.setBaseUrl("https://keycloak.local");

        assertThat(keycloak.isInitialized()).isTrue();

        Proxy proxy = new Proxy();
        proxy.setHost("proxy.local");
        keycloak.setProxy(proxy);

        assertThat(keycloak.useProxy()).isTrue();
    }
}
