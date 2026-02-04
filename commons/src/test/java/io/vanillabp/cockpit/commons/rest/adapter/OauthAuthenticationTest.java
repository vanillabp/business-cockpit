package io.vanillabp.cockpit.commons.rest.adapter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OauthAuthenticationTest {

    @Test
    void extendsClient() {
        OauthAuthentication oauth = new OauthAuthentication();

        assertThat(oauth).isInstanceOf(Client.class);
    }

    @Test
    void defaultValues_areSetCorrectly() {
        OauthAuthentication oauth = new OauthAuthentication();

        assertThat(oauth.getClientId()).isNull();
        assertThat(oauth.getClientSecret()).isNull();
        assertThat(oauth.isBasic()).isFalse();
        // Inherited from Client
        assertThat(oauth.getBaseUrl()).isEqualTo(Client.TO_BE_DEFINED_URL);
    }

    @Test
    void setClientId_updatesValue() {
        OauthAuthentication oauth = new OauthAuthentication();
        oauth.setClientId("my-client-id");

        assertThat(oauth.getClientId()).isEqualTo("my-client-id");
    }

    @Test
    void setClientSecret_updatesValue() {
        OauthAuthentication oauth = new OauthAuthentication();
        oauth.setClientSecret("my-client-secret");

        assertThat(oauth.getClientSecret()).isEqualTo("my-client-secret");
    }

    @Test
    void setBasic_toTrue_updatesValue() {
        OauthAuthentication oauth = new OauthAuthentication();
        oauth.setBasic(true);

        assertThat(oauth.isBasic()).isTrue();
    }

    @Test
    void fullConfiguration_worksCorrectly() {
        OauthAuthentication oauth = new OauthAuthentication();
        oauth.setBaseUrl("https://auth.example.com/oauth/token");
        oauth.setClientId("service-client");
        oauth.setClientSecret("super-secret");
        oauth.setBasic(true);
        oauth.setConnectTimeout(5000);
        oauth.setReadTimeout(10000);

        assertThat(oauth.isInitialized()).isTrue();
        assertThat(oauth.getBaseUrl()).isEqualTo("https://auth.example.com/oauth/token");
        assertThat(oauth.getClientId()).isEqualTo("service-client");
        assertThat(oauth.getClientSecret()).isEqualTo("super-secret");
        assertThat(oauth.isBasic()).isTrue();
        assertThat(oauth.getConnectTimeout()).isEqualTo(5000);
        assertThat(oauth.getReadTimeout()).isEqualTo(10000);
    }

    @Test
    void inheritedMethods_fromClient_workCorrectly() {
        OauthAuthentication oauth = new OauthAuthentication();
        oauth.setBaseUrl("https://auth.server.com");

        assertThat(oauth.isInitialized()).isTrue();

        Proxy proxy = new Proxy();
        proxy.setHost("proxy.local");
        oauth.setProxy(proxy);

        assertThat(oauth.useProxy()).isTrue();
    }
}
